package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.EditorTool;
import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapEditorService;
import fr.heneria.zombie.core.editor.MapPoint;
import fr.heneria.zombie.plugin.gui.GuiId;
import fr.heneria.zombie.plugin.gui.GuiService;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/** Converts protected-tool block clicks into editor service mutations. */
public final class EditorPlacementListener implements Listener {
  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final MapEditorService editors;
  private final EditorItemService items;
  private final GuiService guis;
  private final Clock clock;
  private final Executor mainThread;

  public EditorPlacementListener(
      MapEditorService editors,
      EditorItemService items,
      GuiService guis,
      Clock clock,
      Executor mainThread) {
    this.editors = editors;
    this.items = items;
    this.guis = guis;
    this.clock = clock;
    this.mainThread = mainThread;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInteract(PlayerInteractEvent event) {
    if (!items.isTool(event.getItem())) {
      return;
    }
    var session = editors.session(event.getPlayer().getUniqueId()).orElse(null);
    if (session == null) {
      return;
    }
    event.setCancelled(true);
    if (event.getAction() == Action.RIGHT_CLICK_AIR
        || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      guis.openHome(event.getPlayer(), new GuiId("editor-main"));
      return;
    }
    if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) {
      return;
    }
    MapPoint point = point(event.getClickedBlock().getLocation().add(0.5, 1, 0.5));
    EditorTool tool = session.tool();
    if (tool == EditorTool.NONE) {
      event.getPlayer().sendMessage(MINI.deserialize("<yellow>Sélectionnez un outil dans le GUI."));
      return;
    }
    editors
        .mutate(
            event.getPlayer().getUniqueId(), definition -> apply(definition, session, tool, point))
        .whenCompleteAsync(
            (definition, failure) -> {
              if (failure == null) {
                event
                    .getPlayer()
                    .sendMessage(
                        MINI.deserialize("<green>Modification sauvegardée automatiquement."));
              } else {
                event
                    .getPlayer()
                    .sendMessage(
                        MINI.deserialize("<red>Placement refusé : " + failure.getMessage()));
              }
            },
            mainThread);
  }

  @EventHandler(ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    if (items.isTool(event.getItemDrop().getItemStack())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInventory(InventoryClickEvent event) {
    boolean hotbarTool =
        event.getHotbarButton() >= 0
            && items.isTool(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
    if (items.isTool(event.getCurrentItem()) || items.isTool(event.getCursor()) || hotbarTool) {
      event.setCancelled(true);
    }
  }

  private MapDefinition apply(
      MapDefinition map,
      fr.heneria.zombie.core.editor.MapEditorSession session,
      EditorTool tool,
      MapPoint point) {
    String id = nextId(map, tool);
    String zone = selectedZone(map, session.selectedZone());
    return switch (tool) {
      case SET_PLAYER_SPAWN -> map.withPlayerSpawn(point, clock.instant());
      case ADD_ZONE ->
          map.withZone(new MapDefinition.Zone(id, id, "AQUA", "", "", 1.0, point), clock.instant());
      case ADD_DOOR -> {
        if (map.zones().size() < 2) {
          throw new IllegalStateException("Deux zones sont nécessaires");
        }
        var zones = map.zones().keySet().stream().toList();
        String source = zone.isBlank() ? zones.getFirst() : zone;
        String target =
            zones.stream().filter(candidate -> !candidate.equals(source)).findFirst().orElseThrow();
        yield map.withDoor(
            new MapDefinition.Door(id, id, 750, source, target, "DOOR", false, "", "", "", point),
            clock.instant());
      }
      case ADD_ZOMBIE_SPAWN -> {
        if (zone.isBlank()) {
          throw new IllegalStateException("Créez d'abord une zone");
        }
        yield map.withZombieSpawn(
            new MapDefinition.ZombieSpawn(
                id,
                id,
                zone,
                1.0,
                4,
                1,
                Integer.MAX_VALUE,
                0,
                128,
                false,
                Set.of("NORMAL"),
                20,
                point),
            clock.instant());
      }
      case ADD_OBJECT -> {
        if (zone.isBlank()
            && session.selectedObjectType() != fr.heneria.zombie.core.editor.MapObjectType.POWER) {
          throw new IllegalStateException("Créez d'abord une zone");
        }
        yield map.withObject(
            new MapDefinition.MapObject(
                id,
                session.selectedObjectType(),
                id,
                zone,
                point,
                defaults(session.selectedObjectType())),
            clock.instant());
      }
      case MOVE -> {
        String kind = session.clipboard().kind();
        String entity = session.clipboard().values().getOrDefault("id", "");
        yield map.moved(kind, entity, point, clock.instant());
      }
      case NONE, MODIFY, DELETE, DUPLICATE ->
          throw new IllegalStateException("Action non positionnable");
    };
  }

  private static Map<String, String> defaults(fr.heneria.zombie.core.editor.MapObjectType type) {
    return switch (type) {
      case BARRICADE -> Map.of("kind", "WINDOW", "hp", "100", "boards", "6");
      case MYSTERY_BOX -> Map.of("cost", "950", "rotation", "0");
      case PACK_A_PUNCH -> Map.of("cost", "5000", "levels", "3");
      case PERK -> Map.of("perk-id", "unassigned", "cost", "2000");
      case TRAP -> Map.of("kind", "DAMAGE", "cost", "1000", "cooldown", "600");
      case TELEPORTER -> Map.of("destination", "unassigned", "cooldown", "600");
      case POWER -> Map.of("enabled", "false");
      case OBJECTIVE -> Map.of("kind", "INTERACT", "target", "unassigned");
      case QUEST -> Map.of("steps", "1");
      case BOSS -> Map.of("rounds", "10", "boss-id", "unassigned");
    };
  }

  private static String selectedZone(MapDefinition map, String selected) {
    return map.zones().containsKey(selected)
        ? selected
        : map.zones().keySet().stream().findFirst().orElse("");
  }

  private static String nextId(MapDefinition map, EditorTool tool) {
    String prefix =
        switch (tool) {
          case ADD_ZONE -> "zone";
          case ADD_DOOR -> "door";
          case ADD_ZOMBIE_SPAWN -> "spawn";
          case ADD_OBJECT -> "object";
          default -> "entity";
        };
    int index = 1;
    while (map.zones().containsKey(prefix + "_" + index)
        || map.doors().containsKey(prefix + "_" + index)
        || map.zombieSpawns().containsKey(prefix + "_" + index)
        || map.objects().containsKey(prefix + "_" + index)) {
      index++;
    }
    return prefix + "_" + index;
  }

  private static MapPoint point(Location location) {
    return new MapPoint(
        location.getWorld().getName(),
        location.getX(),
        location.getY(),
        location.getZ(),
        location.getYaw(),
        location.getPitch());
  }
}
