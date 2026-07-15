package fr.heneria.bedwars.plugin.command;

import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.config.ConfigurationReloadResult;
import fr.heneria.bedwars.core.config.ConfigurationSnapshot;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.plugin.bootstrap.PluginBootstrap;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.DemoMenuFactory;
import fr.heneria.bedwars.plugin.gui.GuiService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Implements the translated Bukkit-compatible administrative command. */
public final class BedWarsCommand implements CommandExecutor, TabCompleter {
  public static final String ADMIN = AdministrativeCommandPolicy.ADMIN;
  public static final String RELOAD = AdministrativeCommandPolicy.RELOAD;
  public static final String CONFIG = AdministrativeCommandPolicy.CONFIG;
  public static final String LANGUAGE = AdministrativeCommandPolicy.LANGUAGE;
  public static final String GUI = AdministrativeCommandPolicy.GUI;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final JavaPlugin plugin;
  private final PluginBootstrap bootstrap;
  private final ConfigurationService configurations;
  private final AdministrativeCommandPolicy completionPolicy = new AdministrativeCommandPolicy();
  private final GuiService guiService;
  private final DemoMenuFactory demoMenus;

  public BedWarsCommand(
      JavaPlugin plugin,
      PluginBootstrap bootstrap,
      ConfigurationService configurations,
      GuiService guiService) {
    this.plugin = plugin;
    this.bootstrap = bootstrap;
    this.configurations = configurations;
    this.guiService = guiService;
    this.demoMenus =
        new DemoMenuFactory(configurations, guiService, plugin.getDescription().getVersion());
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    try {
      if (args.length == 0) return help(sender);
      return switch (args[0].toLowerCase(Locale.ROOT)) {
        case "version" -> version(sender);
        case "reload" -> reload(sender);
        case "config" -> config(sender);
        case "language" -> language(sender, args);
        case "gui" -> gui(sender);
        default -> send(sender, TranslationKey.UNKNOWN_COMMAND);
      };
    } catch (RuntimeException exception) {
      plugin.getLogger().log(java.util.logging.Level.SEVERE, "Command execution failed", exception);
      sender.sendMessage("HeneriaBedWars encountered an internal message error.");
      return true;
    }
  }

  private boolean help(CommandSender sender) {
    if (!allowed(sender, ADMIN)) return true;
    send(sender, TranslationKey.HELP_HEADER);
    send(sender, TranslationKey.HELP_VERSION);
    if (sender.hasPermission(RELOAD)) send(sender, TranslationKey.HELP_RELOAD);
    if (sender.hasPermission(CONFIG)) send(sender, TranslationKey.HELP_CONFIG);
    if (sender.hasPermission(LANGUAGE)) send(sender, TranslationKey.HELP_LANGUAGE);
    if (sender.hasPermission(GUI)) send(sender, TranslationKey.HELP_GUI);
    return true;
  }

  private boolean version(CommandSender sender) {
    if (!allowed(sender, ADMIN)) return true;
    PlaceholderContext context =
        PlaceholderContext.builder()
            .put("plugin_version", plugin.getDescription().getVersion())
            .put("java_version", System.getProperty("java.version"))
            .put("server_version", plugin.getServer().getVersion())
            .put("state", bootstrap.status().name().toLowerCase(Locale.ROOT))
            .put("service_count", bootstrap.serviceCount())
            .build();
    send(sender, TranslationKey.VERSION_HEADER);
    send(sender, TranslationKey.VERSION_PLUGIN, context);
    send(sender, TranslationKey.VERSION_JAVA, context);
    send(sender, TranslationKey.VERSION_SERVER, context);
    send(sender, TranslationKey.VERSION_STATE, context);
    send(sender, TranslationKey.VERSION_SERVICES, context);
    return true;
  }

  private boolean reload(CommandSender sender) {
    if (!allowed(sender, RELOAD)) return true;
    send(sender, TranslationKey.RELOAD_STARTED);
    ConfigurationReloadResult result = configurations.reloadAll();
    PlaceholderContext context =
        PlaceholderContext.builder()
            .put("loaded_files", result.loadedFiles())
            .put("warnings", result.warnings())
            .put("errors", result.errors())
            .build();
    send(
        sender, result.successful() ? TranslationKey.RELOAD_SUCCESS : TranslationKey.RELOAD_FAILED);
    send(sender, TranslationKey.RELOAD_LOADED, context);
    send(sender, TranslationKey.RELOAD_WARNINGS, context);
    send(sender, TranslationKey.RELOAD_ERRORS, context);
    if (!result.successful()) send(sender, TranslationKey.RELOAD_PRESERVED);
    return true;
  }

  private boolean config(CommandSender sender) {
    if (!allowed(sender, CONFIG)) return true;
    ConfigurationSnapshot snapshot = configurations.snapshot();
    String versions =
        "config="
            + snapshot.documents().get(ConfigurationId.GENERAL).version()
            + ", gameplay="
            + snapshot.documents().get(ConfigurationId.GAMEPLAY).version()
            + ", menus="
            + snapshot.documents().get(ConfigurationId.MENUS).version();
    PlaceholderContext context =
        PlaceholderContext.builder()
            .put("language", snapshot.plugin().locale())
            .put("debug", snapshot.plugin().debug())
            .put("loaded_files", snapshot.documents().size() + snapshot.languages().size())
            .put("versions", versions)
            .put("storage", snapshot.storage().type())
            .put("last_reload", DATE.format(snapshot.loadedAt().atZone(ZoneId.systemDefault())))
            .put("warnings", snapshot.warningCount())
            .put("gui_sessions", guiService.openCount())
            .build();
    send(sender, TranslationKey.CONFIG_HEADER);
    send(sender, TranslationKey.CONFIG_LANGUAGE, context);
    send(sender, TranslationKey.CONFIG_DEBUG, context);
    send(sender, TranslationKey.CONFIG_FILES, context);
    send(sender, TranslationKey.CONFIG_VERSIONS, context);
    send(sender, TranslationKey.CONFIG_STORAGE, context);
    send(sender, TranslationKey.CONFIG_LAST_RELOAD, context);
    send(sender, TranslationKey.CONFIG_WARNINGS, context);
    send(sender, TranslationKey.CONFIG_GUI_SESSIONS, context);
    return true;
  }

  private boolean gui(CommandSender sender) {
    if (!allowed(sender, GUI)) return true;
    if (!(sender instanceof Player player)) return send(sender, TranslationKey.PLAYER_ONLY);
    guiService.open(player, demoMenus.main());
    return true;
  }

  private boolean language(CommandSender sender, String[] args) {
    if (!allowed(sender, LANGUAGE)) return true;
    if (args.length == 1) {
      PlaceholderContext context =
          PlaceholderContext.builder()
              .put("language", configurations.snapshot().plugin().locale())
              .put("locales", String.join(", ", configurations.availableLocales()))
              .build();
      send(sender, TranslationKey.LANGUAGE_CURRENT, context);
      send(sender, TranslationKey.LANGUAGE_AVAILABLE, context);
      send(sender, TranslationKey.LANGUAGE_USAGE);
      return true;
    }
    if (args.length != 3 || !args[1].equalsIgnoreCase("set"))
      return send(sender, TranslationKey.LANGUAGE_USAGE);
    String locale = args[2];
    if (!configurations.availableLocales().contains(locale)) {
      return send(
          sender,
          TranslationKey.LANGUAGE_UNKNOWN,
          PlaceholderContext.builder().put("language", locale).build());
    }
    ConfigurationReloadResult result = configurations.setLanguage(locale);
    if (!result.successful()) return send(sender, TranslationKey.RELOAD_FAILED);
    return send(
        sender,
        TranslationKey.LANGUAGE_CHANGED,
        PlaceholderContext.builder().put("language", locale).build());
  }

  private boolean allowed(CommandSender sender, String permission) {
    if (sender.hasPermission(permission)) return true;
    send(sender, TranslationKey.NO_PERMISSION);
    return false;
  }

  private boolean send(CommandSender sender, TranslationKey key) {
    sender.sendMessage(configurations.language().message(key));
    return true;
  }

  private boolean send(CommandSender sender, TranslationKey key, PlaceholderContext context) {
    sender.sendMessage(configurations.language().message(key, context));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    return completionPolicy.complete(
        sender::hasPermission, args, configurations.availableLocales());
  }
}
