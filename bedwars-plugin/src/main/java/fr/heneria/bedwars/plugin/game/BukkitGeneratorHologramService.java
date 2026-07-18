package fr.heneria.bedwars.plugin.game;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import fr.heneria.bedwars.core.game.generator.GeneratorId;
import fr.heneria.bedwars.core.game.generator.GeneratorResource;
import fr.heneria.bedwars.core.game.generator.RuntimeGenerator;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Centralized text displays for diamond and emerald generator countdowns. */
public final class BukkitGeneratorHologramService {
  private final ConfigurationService configurations;
  private final BukkitGeneratorCatalog catalog;
  private final NamespacedKey gameKey;
  private final NamespacedKey generatorKey;
  private final Map<Key, TextDisplay> displays = new LinkedHashMap<>();
  private final Map<Key, String> rendered = new LinkedHashMap<>();

  public BukkitGeneratorHologramService(
      JavaPlugin plugin, ConfigurationService configurations, BukkitGeneratorCatalog catalog) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    gameKey = new NamespacedKey(plugin, "generator_hologram_game_id");
    generatorKey = new NamespacedKey(plugin, "generator_hologram_id");
  }

  public void initialize(GameInstance game, Instant now) {
    requireMainThread();
    remove(game.id());
    if (!catalog.hologramsEnabled()) return;
    var world = game.world().map(handle -> Bukkit.getWorld(handle.worldName())).orElse(null);
    if (world == null) return;
    world.getEntities().stream()
        .filter(
            entity ->
                game.id()
                    .toString()
                    .equals(
                        entity
                            .getPersistentDataContainer()
                            .get(gameKey, PersistentDataType.STRING)))
        .forEach(Entity::remove);
    for (RuntimeGenerator generator : game.generators()) {
      if (!visible(generator.definition().resource())) continue;
      var position = generator.definition().location();
      Location location =
          new Location(
              world,
              Math.floor(position.x()) + 0.5,
              position.y() + catalog.hologramHeight(),
              Math.floor(position.z()) + 0.5);
      location.getChunk().load();
      TextDisplay display =
          world.spawn(
              location,
              TextDisplay.class,
              value -> {
                value.setBillboard(Display.Billboard.CENTER);
                value.setAlignment(TextDisplay.TextAlignment.CENTER);
                value.setShadowed(true);
                value.setSeeThrough(true);
                value.setLineWidth(220);
                value.setViewRange(32f);
                value.setInvulnerable(true);
                value.setPersistent(false);
                value
                    .getPersistentDataContainer()
                    .set(gameKey, PersistentDataType.STRING, game.id().toString());
                value
                    .getPersistentDataContainer()
                    .set(
                        generatorKey,
                        PersistentDataType.STRING,
                        generator.definition().id().value());
              });
      Key key = new Key(game.id(), generator.definition().id());
      displays.put(key, display);
      update(key, display, generator, now);
    }
  }

  public void refresh(Collection<GameInstance> games, Instant now) {
    requireMainThread();
    if (!catalog.hologramsEnabled()) {
      clear();
      return;
    }
    for (GameInstance game : games) {
      if (game.state() != GameState.PLAYING) continue;
      for (RuntimeGenerator generator : game.generators()) {
        if (!visible(generator.definition().resource())) continue;
        Key key = new Key(game.id(), generator.definition().id());
        TextDisplay display = displays.get(key);
        if (display == null || !display.isValid()) continue;
        update(key, display, generator, now);
      }
    }
    displays.entrySet().removeIf(entry -> !entry.getValue().isValid());
    rendered.keySet().retainAll(displays.keySet());
  }

  public void remove(GameId gameId) {
    displays
        .entrySet()
        .removeIf(
            entry -> {
              if (!entry.getKey().gameId().equals(gameId)) return false;
              if (entry.getValue().isValid()) entry.getValue().remove();
              rendered.remove(entry.getKey());
              return true;
            });
  }

  public void clear() {
    displays.values().stream().filter(Entity::isValid).forEach(Entity::remove);
    displays.clear();
    rendered.clear();
  }

  private void update(Key key, TextDisplay display, RuntimeGenerator generator, Instant now) {
    long millis = Math.max(0L, Duration.between(now, generator.nextEmissionAt()).toMillis());
    long seconds = (millis + 999L) / 1000L;
    String text =
        configurations
            .language()
            .message(
                "generator.hologram."
                    + generator.definition().resource().name().toLowerCase(java.util.Locale.ROOT),
                configurations.snapshot().plugin().locale(),
                PlaceholderContext.builder()
                    .put("level", generator.definition().level())
                    .put("seconds", seconds)
                    .build());
    if (text.equals(rendered.put(key, text))) return;
    display.setText(text);
  }

  private static boolean visible(GeneratorResource resource) {
    return resource == GeneratorResource.DIAMOND || resource == GeneratorResource.EMERALD;
  }

  private static void requireMainThread() {
    if (!Bukkit.isPrimaryThread())
      throw new IllegalStateException("Generator holograms require the server thread");
  }

  private record Key(GameId gameId, GeneratorId generatorId) {}
}
