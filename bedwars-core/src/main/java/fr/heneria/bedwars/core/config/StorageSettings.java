package fr.heneria.bedwars.core.config;

/** Immutable storage settings; SQLite is active while network backends remain preparatory. */
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
