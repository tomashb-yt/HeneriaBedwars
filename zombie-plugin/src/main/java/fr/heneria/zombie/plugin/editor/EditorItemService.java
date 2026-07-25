package fr.heneria.zombie.plugin.editor;

import fr.heneria.zombie.core.editor.MapEditorSession;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Creates and recognizes the protected in-game placement tool. */
public final class EditorItemService {
  private final NamespacedKey key;

  public EditorItemService(JavaPlugin plugin) {
    key = new NamespacedKey(plugin, "map_editor_tool");
  }

  public void give(Player player, MapEditorSession session) {
    ItemStack item = new ItemStack(Material.BLAZE_ROD);
    var meta = item.getItemMeta();
    var mini = MiniMessage.miniMessage();
    meta.displayName(mini.deserialize("<gold>Outil éditeur Zombies"));
    meta.lore(
        List.of(
            mini.deserialize("<gray>Map : <white>" + session.mapId()),
            mini.deserialize("<gray>Sélectionnez une action dans le GUI."),
            mini.deserialize("<yellow>Cliquez ensuite sur un bloc.")));
    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    item.setItemMeta(meta);
    player.getInventory().setItem(8, item);
  }

  public boolean isTool(ItemStack item) {
    return item != null
        && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
  }

  public void remove(Player player) {
    for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
      if (isTool(player.getInventory().getItem(slot))) {
        player.getInventory().setItem(slot, null);
      }
    }
  }
}
