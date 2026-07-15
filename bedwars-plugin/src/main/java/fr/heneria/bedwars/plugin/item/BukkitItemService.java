package fr.heneria.bedwars.plugin.item;

import fr.heneria.bedwars.core.config.ConfigurationReloadResult;
import fr.heneria.bedwars.core.item.ItemContext;
import fr.heneria.bedwars.core.item.ItemDefinition;
import fr.heneria.bedwars.core.item.ItemReloadResult;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.Optional;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/** Snapshot-backed item service with a centralized safe fallback. */
public final class BukkitItemService implements ItemService {
  private final ConfigurationService configurations;
  private final BukkitItemFactory factory;
  private final ProjectLogger logger;

  public BukkitItemService(
      JavaPlugin plugin, ConfigurationService configurations, ProjectLogger logger) {
    this.configurations = configurations;
    this.logger = logger;
    factory = new BukkitItemFactory(plugin, configurations.language());
  }

  @Override
  public ItemStack build(String key, ItemContext context) {
    return factory.build(requireDefinition(key), context);
  }

  @Override
  public ItemStack buildOrFallback(String key, ItemContext context) {
    try {
      return build(key, context);
    } catch (RuntimeException exception) {
      logger.error("[Items] Unable to build '" + key + "'; safe fallback applied.", exception);
      ItemContext.Builder fallback =
          ItemContext.builder()
              .placeholder("item_key", configurations.snapshot().plugin().debug() ? key : "hidden");
      context.playerId().ifPresent(id -> fallback.player(id, context.playerName().orElse("")));
      context.locale().ifPresent(fallback::locale);
      return factory.build(configurations.snapshot().items().fallback(), fallback.build());
    }
  }

  @Override
  public Optional<ItemDefinition> findDefinition(String key) {
    return configurations.snapshot().items().find(key);
  }

  @Override
  public ItemDefinition requireDefinition(String key) {
    return configurations.snapshot().items().require(key);
  }

  @Override
  public Set<String> registeredKeys() {
    return configurations.snapshot().items().keys();
  }

  @Override
  public boolean exists(String key) {
    return findDefinition(key).isPresent();
  }

  @Override
  public ItemReloadResult reload() {
    ConfigurationReloadResult result = configurations.reloadAll();
    return new ItemReloadResult(
        result.successful(), configurations.snapshot().items().size(), result.problems());
  }
}
