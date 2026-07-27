package fr.heneria.zombie.plugin.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Loads, validates and atomically activates cached {@code guis.yml} snapshots off-thread.
 *
 * <p>Bundled defaults are available immediately. A rejected user candidate never replaces the
 * previous valid snapshot.
 */
public final class GuiConfigurationService {

  private static final String RESOURCE = "guis.yml";
  private static final int CURRENT_SCHEMA = 4;
  private final Path dataDirectory;
  private final ClassLoader resourceLoader;
  private final Executor ioExecutor;
  private final Supplier<Set<String>> knownActions;
  private final Consumer<String> diagnostic;
  private final AtomicReference<GuiConfigurationSnapshot> active;
  private final List<Runnable> reloadListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

  /**
   * Creates the configuration service from validated bundled defaults.
   *
   * @param dataDirectory plugin data directory
   * @param resourceLoader bundled resource loader
   * @param ioExecutor bounded file executor
   * @param knownActions currently registered action identifiers
   * @param diagnostic diagnostic sink
   */
  public GuiConfigurationService(
      Path dataDirectory,
      ClassLoader resourceLoader,
      Executor ioExecutor,
      Supplier<Set<String>> knownActions,
      Consumer<String> diagnostic) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
    this.ioExecutor = Objects.requireNonNull(ioExecutor, "ioExecutor");
    this.knownActions = Objects.requireNonNull(knownActions, "knownActions");
    this.diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    try {
      this.active = new AtomicReference<>(parse(bundledYaml()));
    } catch (IOException | GuiConfigurationException failure) {
      throw new IllegalStateException("Bundled GUI configuration is invalid", failure);
    }
  }

  /**
   * Installs and loads the user candidate asynchronously.
   *
   * @return activation future
   */
  public CompletableFuture<GuiConfigurationSnapshot> initializeAsync() {
    return reloadAsync(true);
  }

  /**
   * Reloads a user candidate asynchronously.
   *
   * @return activated snapshot
   */
  public CompletableFuture<GuiConfigurationSnapshot> reloadAsync() {
    return reloadAsync(false);
  }

  /**
   * @return current valid snapshot
   */
  public GuiConfigurationSnapshot current() {
    return active.get();
  }

  /**
   * Adds a cache invalidation listener.
   *
   * @param listener listener
   */
  public void addReloadListener(Runnable listener) {
    reloadListeners.add(Objects.requireNonNull(listener, "listener"));
  }

  private CompletableFuture<GuiConfigurationSnapshot> reloadAsync(boolean install) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Files.createDirectories(dataDirectory);
            Path target = dataDirectory.resolve(RESOURCE);
            if (install && Files.notExists(target)) {
              try (InputStream input = resourceLoader.getResourceAsStream(RESOURCE)) {
                if (input == null) {
                  throw new IOException("Missing bundled " + RESOURCE);
                }
                Files.copy(input, target);
              }
            }
            YamlConfiguration candidate = YamlConfiguration.loadConfiguration(target.toFile());
            migrateLegacy(candidate);
            YamlConfiguration defaults = bundledYaml();
            mergeMissingLeaves(candidate, defaults);
            GuiConfigurationSnapshot snapshot = parse(candidate);
            active.set(snapshot);
            reloadListeners.forEach(Runnable::run);
            return snapshot;
          } catch (IOException | GuiConfigurationException failure) {
            diagnostic.accept("GUI configuration rejected: " + failure.getMessage());
            throw new CompletionException(failure);
          }
        },
        ioExecutor);
  }

  /**
   * Migrates identifiers shipped by the first GUI schema before new defaults are merged.
   *
   * <p>Without this migration, the removed {@code group} button remains at slot 24 while the
   * current {@code leave} button is added to the same slot. Only legacy buttons still pointing to
   * their bundled action are removed, so unrelated custom buttons are preserved.
   */
  private static void migrateLegacy(YamlConfiguration candidate) {
    int schema = candidate.getInt("schema-version", 1);
    if (schema < 2) {
      removeLegacyButton(candidate, "join", "nav.instances");
      removeLegacyButton(candidate, "group", "feedback.unavailable");
      removeLegacyButton(candidate, "profile", "feedback.unavailable");
      String playAction = candidate.getString("menus.player-main.buttons.play.actions.left", "");
      if ("nav.instances".equals(playAction)) {
        candidate.set("menus.player-main.buttons.play.actions.left", "maps.player");
        candidate.set("menus.player-main.buttons.play.permission", "zombies.menu.player");
      }
    }
    if (schema < 3) {
      String mapsAction = candidate.getString("menus.admin-main.buttons.maps.actions.left", "");
      if ("nav.maps".equals(mapsAction)) {
        candidate.set("menus.admin-main.buttons.maps.actions.left", "maps.admin");
        candidate.set("menus.admin-main.buttons.maps.permission", "zombies.admin.maps.view");
      }
    }
    if (schema < 4) {
      String detail = "menus.admin-map-detail";
      candidate.set(detail + ".title", null);
      candidate.set(detail + ".size", null);
      for (String button :
          List.of(
              "visit",
              "edit",
              "duplicate",
              "validate",
              "test",
              "publish",
              "unpublish",
              "history",
              "archive",
              "delete",
              "back",
              "home")) {
        candidate.set(detail + ".buttons." + button, null);
      }
    }
    candidate.set("schema-version", CURRENT_SCHEMA);
  }

  private static void removeLegacyButton(
      YamlConfiguration candidate, String button, String bundledAction) {
    String path = "menus.player-main.buttons." + button;
    if (bundledAction.equals(candidate.getString(path + ".actions.left", ""))) {
      candidate.set(path, null);
    }
  }

  private YamlConfiguration bundledYaml() throws IOException {
    try (InputStream input = resourceLoader.getResourceAsStream(RESOURCE)) {
      if (input == null) {
        throw new IOException("Missing bundled " + RESOURCE);
      }
      return YamlConfiguration.loadConfiguration(
          new InputStreamReader(input, StandardCharsets.UTF_8));
    }
  }

  /**
   * Adds every missing scalar or list from the bundled configuration.
   *
   * <p>Bukkit's defaults view does not deeply materialize newly introduced configuration sections.
   * A pre-editor {@code guis.yml}, for example, exposed the new button identifiers while leaving
   * their slot and material absent. Explicit leaf merging preserves every user value and provides
   * complete new menus in memory.
   */
  private static void mergeMissingLeaves(YamlConfiguration candidate, YamlConfiguration defaults) {
    for (String path : defaults.getKeys(true)) {
      Object value = defaults.get(path);
      if (!(value instanceof ConfigurationSection) && !candidate.isSet(path)) {
        candidate.set(path, value);
      }
    }
  }

  private GuiConfigurationSnapshot parse(YamlConfiguration yaml) throws GuiConfigurationException {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Map<String, GuiTheme> themes = parseThemes(yaml, errors);
    String defaultTheme = yaml.getString("default-theme", "dark");
    if (!themes.containsKey(defaultTheme)) {
      errors.add("default-theme references unknown theme " + defaultTheme);
    }
    Map<GuiId, GuiMenuTemplate> menus = parseMenus(yaml, themes.keySet(), errors, warnings);
    if (errors.isEmpty() && menus.isEmpty()) {
      errors.add("menus must contain at least one menu");
    }
    if (!errors.isEmpty()) {
      throw new GuiConfigurationException(errors);
    }
    return new GuiConfigurationSnapshot(defaultTheme, themes, menus, warnings);
  }

  private Map<String, GuiTheme> parseThemes(YamlConfiguration yaml, List<String> errors) {
    Map<String, GuiTheme> themes = new LinkedHashMap<>();
    ConfigurationSection section = yaml.getConfigurationSection("themes");
    if (section == null) {
      errors.add("themes section is missing");
      return themes;
    }
    for (String id : section.getKeys(false)) {
      String path = "themes." + id;
      Material background =
          material(
              section.getString(id + ".background.material", ""), path + ".background", errors);
      String backgroundName = section.getString(id + ".background.name", " ");
      Map<String, String> colors = new LinkedHashMap<>();
      ConfigurationSection colorSection = section.getConfigurationSection(id + ".colors");
      if (colorSection != null) {
        colorSection
            .getKeys(false)
            .forEach(key -> colors.put(key, colorSection.getString(key, "")));
      }
      if (background != null) {
        themes.put(id, new GuiTheme(id, background, backgroundName, colors));
      }
    }
    return themes;
  }

  private Map<GuiId, GuiMenuTemplate> parseMenus(
      YamlConfiguration yaml, Set<String> themeIds, List<String> errors, List<String> warnings) {
    Map<GuiId, GuiMenuTemplate> menus = new LinkedHashMap<>();
    ConfigurationSection section = yaml.getConfigurationSection("menus");
    if (section == null) {
      errors.add("menus section is missing");
      return menus;
    }
    for (String rawId : section.getKeys(false)) {
      String path = "menus." + rawId;
      GuiId id;
      try {
        id = new GuiId(rawId);
      } catch (IllegalArgumentException invalid) {
        errors.add(path + " has an invalid id");
        continue;
      }
      int size = section.getInt(rawId + ".size", 54);
      if (size < 9 || size > 54 || size % 9 != 0) {
        errors.add(path + ".size must be a multiple of 9 from 9 to 54");
      }
      String theme = section.getString(rawId + ".theme", "");
      if (!themeIds.contains(theme)) {
        errors.add(path + ".theme references unknown theme " + theme);
      }
      List<Integer> contentSlots =
          section.getIntegerList(rawId + ".content-slots").stream().distinct().toList();
      for (int slot : contentSlots) {
        if (slot < 0 || slot >= size) {
          errors.add(path + ".content-slots contains invalid slot " + slot);
        }
      }
      Map<String, GuiButtonTemplate> buttons =
          parseButtons(section.getConfigurationSection(rawId + ".buttons"), path, size, errors);
      Set<Integer> occupied = new LinkedHashSet<>();
      buttons.forEach(
          (key, button) -> {
            if (!occupied.add(button.slot())) {
              errors.add(path + ".buttons has a collision at slot " + button.slot());
            }
            if (contentSlots.contains(button.slot())) {
              warnings.add(path + ".buttons." + key + " overlaps a content slot");
            }
          });
      menus.put(
          id,
          new GuiMenuTemplate(
              id,
              section.getString(rawId + ".title", "<red>" + rawId),
              size,
              theme,
              contentSlots,
              buttons));
    }
    return menus;
  }

  private Map<String, GuiButtonTemplate> parseButtons(
      ConfigurationSection section, String menuPath, int size, List<String> errors) {
    Map<String, GuiButtonTemplate> buttons = new LinkedHashMap<>();
    if (section == null) {
      return buttons;
    }
    for (String key : section.getKeys(false)) {
      String path = menuPath + ".buttons." + key;
      int slot = section.getInt(key + ".slot", -1);
      if (slot < 0 || slot >= size) {
        errors.add(path + ".slot is outside the inventory");
      }
      Material material = material(section.getString(key + ".material", ""), path, errors);
      String left = section.getString(key + ".actions.left", "");
      String right = section.getString(key + ".actions.right", "");
      String shift = section.getString(key + ".actions.shift", "");
      validateAction(left, path + ".actions.left", errors);
      validateAction(right, path + ".actions.right", errors);
      validateAction(shift, path + ".actions.shift", errors);
      Sound sound = sound(section.getString(key + ".sound", ""), path + ".sound", errors);
      if (material != null) {
        buttons.put(
            key,
            new GuiButtonTemplate(
                slot,
                material,
                section.getString(key + ".name", "<white>" + key),
                section.getStringList(key + ".lore"),
                section.getString(key + ".permission", ""),
                section.getBoolean(key + ".show-when-locked", true),
                left,
                right,
                shift,
                sound));
      }
    }
    return buttons;
  }

  private void validateAction(String action, String path, List<String> errors) {
    if (!action.isBlank() && !knownActions.get().contains(action)) {
      errors.add(path + " references unknown action " + action);
    }
  }

  private static Material material(String value, String path, List<String> errors) {
    Material material = Material.matchMaterial(value);
    if (material == null || material == Material.AIR) {
      errors.add(path + " has invalid material " + value);
      return null;
    }
    return material;
  }

  private static Sound sound(String value, String path, List<String> errors) {
    if (value.isBlank()) {
      return null;
    }
    try {
      return Sound.valueOf(value);
    } catch (IllegalArgumentException invalid) {
      errors.add(path + " has invalid sound " + value);
      return null;
    }
  }
}
