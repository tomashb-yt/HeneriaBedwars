package fr.heneria.bedwars.plugin.statistics;

import fr.heneria.bedwars.core.statistics.CompletedMatchStatistics;
import fr.heneria.bedwars.core.statistics.MatchParticipantStatistics;
import fr.heneria.bedwars.core.statistics.MatchRecordResult;
import fr.heneria.bedwars.core.statistics.PlayerStatistics;
import fr.heneria.bedwars.core.statistics.StatisticsRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Single-writer SQLite repository; no filesystem or JDBC call runs on the Bukkit thread. */
public final class SqliteStatisticsRepository implements StatisticsRepository {
  private static final String UPSERT_PLAYER =
      """
      INSERT INTO player_statistics (
        player_uuid, games_played, wins, kills, deaths, final_kills, beds_destroyed,
        play_time_seconds, current_win_streak, best_win_streak, last_played_at
      ) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(player_uuid) DO UPDATE SET
        games_played = player_statistics.games_played + 1,
        wins = player_statistics.wins + excluded.wins,
        kills = player_statistics.kills + excluded.kills,
        deaths = player_statistics.deaths + excluded.deaths,
        final_kills = player_statistics.final_kills + excluded.final_kills,
        beds_destroyed = player_statistics.beds_destroyed + excluded.beds_destroyed,
        play_time_seconds = player_statistics.play_time_seconds + excluded.play_time_seconds,
        best_win_streak = MAX(
          player_statistics.best_win_streak,
          CASE WHEN excluded.wins = 1 THEN player_statistics.current_win_streak + 1 ELSE 0 END
        ),
        current_win_streak = CASE
          WHEN excluded.wins = 1 THEN player_statistics.current_win_streak + 1 ELSE 0 END,
        last_played_at = excluded.last_played_at
      """;

  private final Path database;
  private final int timeoutMillis;
  private final ExecutorService executor;
  private final AtomicReference<CompletableFuture<Void>> initialization = new AtomicReference<>();

  public SqliteStatisticsRepository(Path database, int timeoutMillis) {
    this.database = Objects.requireNonNull(database, "database").toAbsolutePath().normalize();
    if (timeoutMillis < 1) throw new IllegalArgumentException("timeoutMillis must be positive");
    this.timeoutMillis = timeoutMillis;
    this.executor =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "HeneriaBedWars-Statistics");
              thread.setDaemon(true);
              return thread;
            });
  }

  @Override
  public CompletionStage<Void> initialize() {
    CompletableFuture<Void> existing = initialization.get();
    if (existing != null) return existing;
    CompletableFuture<Void> created =
        CompletableFuture.runAsync(this::initializeBlocking, executor);
    return initialization.compareAndSet(null, created) ? created : initialization.get();
  }

  @Override
  public CompletionStage<MatchRecordResult> record(CompletedMatchStatistics match) {
    Objects.requireNonNull(match, "match");
    return ready()
        .thenCompose(
            ignored -> CompletableFuture.supplyAsync(() -> recordBlocking(match), executor));
  }

  @Override
  public CompletionStage<Optional<PlayerStatistics>> find(UUID playerId) {
    Objects.requireNonNull(playerId, "playerId");
    return ready()
        .thenCompose(
            ignored -> CompletableFuture.supplyAsync(() -> findBlocking(playerId), executor));
  }

  private CompletableFuture<Void> ready() {
    return initialize().toCompletableFuture();
  }

  private void initializeBlocking() {
    try {
      Class.forName("org.sqlite.JDBC");
      Path parent = database.getParent();
      if (parent != null) Files.createDirectories(parent);
      try (Connection connection = connection();
          Statement statement = connection.createStatement()) {
        statement.execute("PRAGMA journal_mode=WAL");
        statement.execute("PRAGMA foreign_keys=ON");
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS processed_matches (
              game_uuid TEXT PRIMARY KEY,
              arena_id TEXT NOT NULL,
              map_template_id TEXT NOT NULL,
              winner_team_id TEXT NOT NULL,
              completed_at INTEGER NOT NULL
            )
            """);
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS player_statistics (
              player_uuid TEXT PRIMARY KEY,
              games_played INTEGER NOT NULL DEFAULT 0,
              wins INTEGER NOT NULL DEFAULT 0,
              kills INTEGER NOT NULL DEFAULT 0,
              deaths INTEGER NOT NULL DEFAULT 0,
              final_kills INTEGER NOT NULL DEFAULT 0,
              beds_destroyed INTEGER NOT NULL DEFAULT 0,
              play_time_seconds INTEGER NOT NULL DEFAULT 0,
              current_win_streak INTEGER NOT NULL DEFAULT 0,
              best_win_streak INTEGER NOT NULL DEFAULT 0,
              last_played_at INTEGER
            )
            """);
      }
    } catch (ClassNotFoundException | IOException | SQLException exception) {
      throw new StatisticsStorageException("Cannot initialize SQLite statistics", exception);
    }
  }

  private MatchRecordResult recordBlocking(CompletedMatchStatistics match) {
    try (Connection connection = connection()) {
      connection.setAutoCommit(false);
      try {
        int inserted = insertMatch(connection, match);
        if (inserted == 0) {
          connection.rollback();
          return MatchRecordResult.ALREADY_RECORDED;
        }
        for (MatchParticipantStatistics participant : match.participants())
          upsert(connection, participant, match.completedAt());
        connection.commit();
        return MatchRecordResult.RECORDED;
      } catch (SQLException exception) {
        connection.rollback();
        throw exception;
      }
    } catch (SQLException exception) {
      throw new StatisticsStorageException(
          "Cannot persist completed match " + match.gameId(), exception);
    }
  }

  private static int insertMatch(Connection connection, CompletedMatchStatistics match)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT OR IGNORE INTO processed_matches "
                + "(game_uuid, arena_id, map_template_id, winner_team_id, completed_at) "
                + "VALUES (?, ?, ?, ?, ?)")) {
      statement.setString(1, match.gameId().toString());
      statement.setString(2, match.arenaId());
      statement.setString(3, match.mapTemplateId());
      statement.setString(4, match.winnerTeamId());
      statement.setLong(5, match.completedAt().toEpochMilli());
      return statement.executeUpdate();
    }
  }

  private static void upsert(
      Connection connection, MatchParticipantStatistics participant, Instant completedAt)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(UPSERT_PLAYER)) {
      int win = participant.winner() ? 1 : 0;
      statement.setString(1, participant.playerId().toString());
      statement.setInt(2, win);
      statement.setInt(3, participant.kills());
      statement.setInt(4, participant.deaths());
      statement.setInt(5, participant.finalKills());
      statement.setInt(6, participant.bedsDestroyed());
      statement.setLong(7, participant.playTimeSeconds());
      statement.setInt(8, win);
      statement.setInt(9, win);
      statement.setLong(10, completedAt.toEpochMilli());
      statement.executeUpdate();
    }
  }

  private Optional<PlayerStatistics> findBlocking(UUID playerId) {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT * FROM player_statistics WHERE player_uuid = ?")) {
      statement.setString(1, playerId.toString());
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) return Optional.empty();
        long lastPlayed = result.getLong("last_played_at");
        boolean hasLastPlayed = !result.wasNull();
        return Optional.of(
            new PlayerStatistics(
                playerId,
                result.getLong("games_played"),
                result.getLong("wins"),
                result.getLong("kills"),
                result.getLong("deaths"),
                result.getLong("final_kills"),
                result.getLong("beds_destroyed"),
                result.getLong("play_time_seconds"),
                result.getLong("current_win_streak"),
                result.getLong("best_win_streak"),
                hasLastPlayed ? Optional.of(Instant.ofEpochMilli(lastPlayed)) : Optional.empty()));
      }
    } catch (SQLException exception) {
      throw new StatisticsStorageException("Cannot read statistics for " + playerId, exception);
    }
  }

  private Connection connection() throws SQLException {
    Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA busy_timeout=" + timeoutMillis);
      statement.execute("PRAGMA foreign_keys=ON");
    }
    return connection;
  }

  @Override
  public void close() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }
}
