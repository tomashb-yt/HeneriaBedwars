package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapDefinition;
import fr.heneria.zombie.core.editor.MapObjectType;
import fr.heneria.zombie.core.editor.MapPoint;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Materializes paid stations and shows non-destructive editor markers.
 *
 * <p>Editor blocks are restored on refresh or exit. Runtime blocks live only in the disposable
 * instance world. Marker displays are non-persistent and owned by one editor session.
 */
public final class MapVisualizationService {
  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final NamespacedKey markerKey;
  private final NamespacedKey ownerKey;
  private final Map<UUID, EditorRendering> editorRenderings = new ConcurrentHashMap<>();

  public MapVisualizationService(JavaPlugin plugin) {
    markerKey = new NamespacedKey(plugin, "editor_map_marker");
    ownerKey = new NamespacedKey(plugin, "editor_marker_owner");
  }

  /** Rebuilds all markers and station blocks visible to an editor. */
  public void refreshEditor(UUID editorId, World world, MapDefinition map) {
    clearEditor(editorId);
    ArrayList<UUID> entities = new ArrayList<>();
    LinkedHashMap<BlockPosition, BlockData> originals = new LinkedHashMap<>();

    map.playerSpawn()
        .ifPresent(
            point ->
                marker(
                    editorId,
                    world,
                    point,
                    Material.LIGHT_BLUE_STAINED_GLASS,
                    "<aqua>Spawn joueur",
                    entities));
    map.zombieSpawns()
        .values()
        .forEach(
            spawn ->
                marker(
                    editorId,
                    world,
                    spawn.position(),
                    Material.ZOMBIE_HEAD,
                    "<green>Spawn zombie : <white>" + spawn.id(),
                    entities));
    map.doors()
        .values()
        .forEach(
            door ->
                marker(
                    editorId,
                    world,
                    door.position(),
                    Material.RED_STAINED_GLASS,
                    "<red>Porte : <white>" + door.id(),
                    entities));
    map.objects()
        .values()
        .forEach(
            object -> {
              if (isStation(object.type())) {
                placeStation(world, object.position(), stationMaterial(object.type()), originals);
                label(
                    editorId,
                    world,
                    object.position(),
                    stationLabel(object.type()) + " : <white>" + object.id(),
                    entities);
              } else if (object.type() == MapObjectType.BARRICADE) {
                marker(
                    editorId,
                    world,
                    object.position(),
                    Material.OAK_FENCE,
                    "<gold>Fenêtre / barricade : <white>" + object.id(),
                    entities);
              }
            });
    editorRenderings.put(
        editorId,
        new EditorRendering(world.getUID(), List.copyOf(entities), Map.copyOf(originals)));
  }

  /** Places gameplay-visible Mystery Box and Pack-a-Punch blocks in a disposable instance. */
  public void materializeRuntime(World world, MapDefinition map) {
    map.objects().values().stream()
        .filter(object -> isStation(object.type()))
        .forEach(
            object -> placeStation(world, object.position(), stationMaterial(object.type()), null));
  }

  /** Removes marker entities and restores editor-world blocks changed by this service. */
  public void clearEditor(UUID editorId) {
    EditorRendering rendering = editorRenderings.remove(editorId);
    if (rendering == null) {
      return;
    }
    rendering
        .entities()
        .forEach(
            id -> {
              Entity entity = Bukkit.getEntity(id);
              if (entity != null) {
                entity.remove();
              }
            });
    World world = Bukkit.getWorld(rendering.worldId());
    if (world != null) {
      rendering
          .originalBlocks()
          .forEach(
              (position, data) ->
                  world
                      .getBlockAt(position.x(), position.y(), position.z())
                      .setBlockData(data, false));
    }
  }

  public void clearAll() {
    List.copyOf(editorRenderings.keySet()).forEach(this::clearEditor);
  }

  static Material stationMaterial(MapObjectType type) {
    return switch (type) {
      case MYSTERY_BOX -> Material.CHEST;
      case PACK_A_PUNCH -> Material.ENDER_CHEST;
      default -> throw new IllegalArgumentException("Not a station: " + type);
    };
  }

  private static boolean isStation(MapObjectType type) {
    return type == MapObjectType.MYSTERY_BOX || type == MapObjectType.PACK_A_PUNCH;
  }

  private static String stationLabel(MapObjectType type) {
    return type == MapObjectType.MYSTERY_BOX ? "<light_purple>Mystery Box" : "<aqua>Pack-a-Punch";
  }

  private static void placeStation(
      World world,
      MapPoint point,
      Material material,
      Map<BlockPosition, BlockData> originalBlocks) {
    Block block = world.getBlockAt(floor(point.x()), floor(point.y()), floor(point.z()));
    if (originalBlocks != null) {
      originalBlocks.putIfAbsent(
          new BlockPosition(block.getX(), block.getY(), block.getZ()),
          block.getBlockData().clone());
    }
    block.setType(material, false);
  }

  private void marker(
      UUID editorId,
      World world,
      MapPoint point,
      Material material,
      String text,
      List<UUID> entities) {
    Location location = new Location(world, floor(point.x()), floor(point.y()), floor(point.z()));
    BlockDisplay display = world.spawn(location, BlockDisplay.class);
    display.setBlock(Bukkit.createBlockData(material));
    display.setGlowing(true);
    configure(editorId, display);
    entities.add(display.getUniqueId());
    label(editorId, world, point, text, entities);
  }

  private void label(UUID editorId, World world, MapPoint point, String text, List<UUID> entities) {
    Location location =
        new Location(
            world, floor(point.x()) + 0.5, floor(point.y()) + 1.35, floor(point.z()) + 0.5);
    TextDisplay label = world.spawn(location, TextDisplay.class);
    label.text(MINI.deserialize(text));
    label.setBillboard(Display.Billboard.CENTER);
    label.setSeeThrough(true);
    label.setShadowed(true);
    label.setDefaultBackground(false);
    configure(editorId, label);
    entities.add(label.getUniqueId());
  }

  private void configure(UUID editorId, Entity entity) {
    entity.setPersistent(false);
    entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
    entity
        .getPersistentDataContainer()
        .set(ownerKey, PersistentDataType.STRING, editorId.toString());
  }

  private static int floor(double value) {
    return (int) Math.floor(value);
  }

  private record BlockPosition(int x, int y, int z) {}

  private record EditorRendering(
      UUID worldId, List<UUID> entities, Map<BlockPosition, BlockData> originalBlocks) {}
}
