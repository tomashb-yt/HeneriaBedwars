package fr.heneria.zombie.core.economy;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Constant-time registry of isolated game economies. */
public final class EconomyService {
  private final ConcurrentHashMap<UUID, GameEconomy> games = new ConcurrentHashMap<>();

  public GameEconomy create(UUID gameId, EconomyPolicy policy) {
    GameEconomy created =
        new GameEconomy(
            java.util.Objects.requireNonNull(gameId, "gameId"),
            java.util.Objects.requireNonNull(policy, "policy"));
    GameEconomy previous = games.putIfAbsent(gameId, created);
    if (previous != null) {
      throw new IllegalStateException("Economy already exists for " + gameId);
    }
    return created;
  }

  public Optional<GameEconomy> find(UUID gameId) {
    return Optional.ofNullable(games.get(gameId));
  }

  public Optional<PlayerWallet.Snapshot> wallet(UUID gameId, UUID playerId) {
    return find(gameId).flatMap(game -> game.wallet(playerId)).map(PlayerWallet::snapshot);
  }

  public Collection<GameEconomy.Snapshot> snapshots() {
    return games.values().stream().map(GameEconomy::snapshot).toList();
  }

  public void remove(UUID gameId) {
    GameEconomy removed = games.remove(gameId);
    if (removed != null) {
      removed.close();
    }
  }

  public void clear() {
    games.keySet().forEach(this::remove);
  }
}
