package fr.heneria.bedwars.core.config;

/** Stable identifiers for the built-in configuration files. */
public enum ConfigurationId {
  GENERAL("config.yml"),
  GAMEPLAY("gameplay.yml"),
  LOBBY("lobby.yml"),
  STORAGE("storage.yml"),
  MENUS("menus.yml"),
  ITEMS("items.yml"),
  SHOPS("shops.yml"),
  UPGRADES("upgrades.yml"),
  GENERATORS("generators.yml");

  private final String fileName;

  ConfigurationId(String fileName) {
    this.fileName = fileName;
  }

  public String fileName() {
    return fileName;
  }
}
