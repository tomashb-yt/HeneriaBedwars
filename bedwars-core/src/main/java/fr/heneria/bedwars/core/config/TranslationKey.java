package fr.heneria.bedwars.core.config;

/** Every message used by the currently available commands. */
public enum TranslationKey {
  PREFIX("general.prefix"),
  NO_PERMISSION("general.no-permission"),
  UNKNOWN_COMMAND("general.unknown-command"),
  INTERNAL_ERROR("general.internal-error"),
  HELP_HEADER("command.help.header"),
  HELP_VERSION("command.help.version"),
  HELP_RELOAD("command.help.reload"),
  HELP_CONFIG("command.help.config"),
  HELP_LANGUAGE("command.help.language"),
  VERSION_HEADER("command.version.header"),
  VERSION_PLUGIN("command.version.plugin-version"),
  VERSION_JAVA("command.version.java-version"),
  VERSION_SERVER("command.version.server-version"),
  VERSION_STATE("command.version.state"),
  VERSION_SERVICES("command.version.services"),
  RELOAD_STARTED("command.reload.started"),
  RELOAD_SUCCESS("command.reload.success"),
  RELOAD_FAILED("command.reload.failed"),
  RELOAD_LOADED("command.reload.loaded-files"),
  RELOAD_WARNINGS("command.reload.warnings"),
  RELOAD_ERRORS("command.reload.errors"),
  RELOAD_PRESERVED("command.reload.preserved"),
  CONFIG_HEADER("command.config.header"),
  CONFIG_LANGUAGE("command.config.language"),
  CONFIG_DEBUG("command.config.debug"),
  CONFIG_FILES("command.config.loaded-files"),
  CONFIG_VERSIONS("command.config.versions"),
  CONFIG_STORAGE("command.config.storage"),
  CONFIG_LAST_RELOAD("command.config.last-reload"),
  CONFIG_WARNINGS("command.config.warnings"),
  LANGUAGE_CURRENT("command.language.current"),
  LANGUAGE_AVAILABLE("command.language.available"),
  LANGUAGE_USAGE("command.language.usage"),
  LANGUAGE_CHANGED("command.language.changed"),
  LANGUAGE_UNKNOWN("command.language.unknown");

  private final String path;

  TranslationKey(String path) {
    this.path = path;
  }

  public String path() {
    return path;
  }
}
