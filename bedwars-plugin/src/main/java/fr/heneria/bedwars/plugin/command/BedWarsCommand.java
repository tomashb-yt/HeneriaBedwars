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
import fr.heneria.bedwars.plugin.item.ItemContexts;
import fr.heneria.bedwars.plugin.item.ItemPreviewMenuFactory;
import fr.heneria.bedwars.plugin.item.ItemService;
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
  public static final String ITEM = AdministrativeCommandPolicy.ITEM;
  public static final String ITEM_GIVE = AdministrativeCommandPolicy.ITEM_GIVE;
  public static final String ITEM_PREVIEW = AdministrativeCommandPolicy.ITEM_PREVIEW;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final JavaPlugin plugin;
  private final PluginBootstrap bootstrap;
  private final ConfigurationService configurations;
  private final AdministrativeCommandPolicy completionPolicy = new AdministrativeCommandPolicy();
  private final GuiService guiService;
  private final ItemService itemService;
  private final DemoMenuFactory demoMenus;
  private final ItemPreviewMenuFactory itemPreview;

  public BedWarsCommand(
      JavaPlugin plugin,
      PluginBootstrap bootstrap,
      ConfigurationService configurations,
      ItemService itemService,
      GuiService guiService) {
    this.plugin = plugin;
    this.bootstrap = bootstrap;
    this.configurations = configurations;
    this.guiService = guiService;
    this.itemService = itemService;
    this.demoMenus =
        new DemoMenuFactory(configurations, guiService, plugin.getDescription().getVersion());
    this.itemPreview =
        new ItemPreviewMenuFactory(
            configurations, itemService, plugin.getDescription().getVersion());
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
        case "item" -> item(sender, args);
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
    if (sender.hasPermission(ITEM)) send(sender, TranslationKey.HELP_ITEM);
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
            .put("items", snapshot.items().size())
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
    send(sender, TranslationKey.CONFIG_ITEMS, context);
    return true;
  }

  private boolean gui(CommandSender sender) {
    if (!allowed(sender, GUI)) return true;
    if (!(sender instanceof Player player)) return send(sender, TranslationKey.PLAYER_ONLY);
    guiService.open(player, demoMenus.main());
    return true;
  }

  private boolean item(CommandSender sender, String[] args) {
    if (!allowed(sender, ITEM)) return true;
    if (args.length == 1) return send(sender, TranslationKey.ITEM_HELP);
    return switch (args[1].toLowerCase(Locale.ROOT)) {
      case "list" -> itemList(sender);
      case "give" -> itemGive(sender, args);
      case "preview" -> itemPreview(sender);
      default -> send(sender, TranslationKey.ITEM_HELP);
    };
  }

  private boolean itemList(CommandSender sender) {
    List<String> keys = itemService.registeredKeys().stream().sorted().toList();
    String visible = String.join(", ", keys.stream().limit(20).toList());
    if (keys.size() > 20) visible += " …";
    return send(
        sender,
        TranslationKey.ITEM_LIST,
        PlaceholderContext.builder().put("count", keys.size()).put("keys", visible).build());
  }

  private boolean itemGive(CommandSender sender, String[] args) {
    if (!allowed(sender, ITEM_GIVE)) return true;
    if (!(sender instanceof Player player)) return send(sender, TranslationKey.PLAYER_ONLY);
    if (args.length != 3 || !itemService.exists(args[2]))
      return send(
          sender,
          TranslationKey.ITEM_UNKNOWN,
          PlaceholderContext.builder().put("item_key", args.length >= 3 ? args[2] : "?").build());
    if (player.getInventory().firstEmpty() < 0)
      return send(sender, TranslationKey.ITEM_INVENTORY_FULL);
    player
        .getInventory()
        .addItem(
            itemService.build(
                args[2],
                ItemContexts.forPlayer(player, configurations)
                    .placeholder("plugin_version", plugin.getDescription().getVersion())
                    .build()));
    return send(
        sender,
        TranslationKey.ITEM_GIVEN,
        PlaceholderContext.builder().put("item_key", args[2]).build());
  }

  private boolean itemPreview(CommandSender sender) {
    if (!allowed(sender, ITEM_PREVIEW)) return true;
    if (!(sender instanceof Player player)) return send(sender, TranslationKey.PLAYER_ONLY);
    guiService.open(player, itemPreview.create());
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
        sender::hasPermission,
        args,
        configurations.availableLocales(),
        itemService.registeredKeys().stream().sorted().toList());
  }
}
