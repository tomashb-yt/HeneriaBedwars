package fr.heneria.bedwars.core.config;

/** Immutable preparatory storage settings. */
public record StorageSettings(
    String type,
    String sqliteFile,
    String mysqlHost,
    int mysqlPort,
    String mysqlDatabase,
    String mysqlUsername,
    String mysqlPassword,
    boolean mysqlUseSsl,
    int connectionTimeoutMillis,
    boolean redisEnabled,
    String redisHost,
    int redisPort) {}
