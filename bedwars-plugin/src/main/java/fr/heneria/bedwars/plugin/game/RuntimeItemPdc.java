package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.core.game.item.RuntimeItemAction;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Reads and writes the stable runtime item identity stored in Bukkit PDC. */
final class RuntimeItemPdc {
  record Token(String actionKey, String gameId) {}

  private final NamespacedKey runtimeItemKey;
  private final NamespacedKey gameIdKey;

  RuntimeItemPdc(JavaPlugin plugin) {
    runtimeItemKey = new NamespacedKey(plugin, "runtime_item");
    gameIdKey = new NamespacedKey(plugin, "runtime_game_id");
  }

  ItemStack tag(ItemStack stack, RuntimeItemAction action, String gameId) {
    ItemMeta meta = stack.getItemMeta();
    if (meta == null) return stack;
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    pdc.set(runtimeItemKey, PersistentDataType.STRING, action.key());
    pdc.set(gameIdKey, PersistentDataType.STRING, gameId);
    stack.setItemMeta(meta);
    return stack;
  }

  Optional<Token> read(ItemStack stack) {
    if (stack == null || !stack.hasItemMeta()) return Optional.empty();
    PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
    String action = pdc.get(runtimeItemKey, PersistentDataType.STRING);
    String gameId = pdc.get(gameIdKey, PersistentDataType.STRING);
    return action == null ? Optional.empty() : Optional.of(new Token(action, gameId));
  }
}
