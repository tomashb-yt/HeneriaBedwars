package fr.heneria.bedwars.core.game.upgrade;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.game.GameInstanceManager;
import fr.heneria.bedwars.core.game.event.GameEventBus;
import fr.heneria.bedwars.core.game.event.TeamUpgradePurchaseEvent;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Pure team-upgrade purchase policy with one atomic platform payment. */
public final class TeamUpgradePurchaseService {
  private final GameInstanceManager games;
  private final GameEventBus events;
  private final Clock clock;

  public TeamUpgradePurchaseService(GameInstanceManager games, GameEventBus events, Clock clock) {
    this.games = Objects.requireNonNull(games, "games");
    this.events = Objects.requireNonNull(events, "events");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public TeamUpgradePurchaseResult purchase(
      UUID playerId, TeamUpgradeDefinition definition, TeamUpgradeWallet wallet) {
    var game = games.byPlayer(playerId).orElse(null);
    if (game == null) return result(TeamUpgradePurchaseCode.NOT_IN_GAME, definition, 0, 0, wallet);
    if (game.state() != GameState.PLAYING)
      return result(TeamUpgradePurchaseCode.NOT_PLAYING, definition, 0, 0, wallet);
    var player = game.player(playerId).orElse(null);
    if (player == null)
      return result(TeamUpgradePurchaseCode.NOT_IN_GAME, definition, 0, 0, wallet);
    if (player.spectator())
      return result(TeamUpgradePurchaseCode.SPECTATOR, definition, 0, 0, wallet);
    var team = player.teamId().flatMap(game::team).orElse(null);
    if (team == null) return result(TeamUpgradePurchaseCode.NOT_IN_GAME, definition, 0, 0, wallet);
    int previous = team.upgradeLevel(definition.type());
    if (previous >= definition.maximumLevel())
      return result(TeamUpgradePurchaseCode.MAX_LEVEL, definition, previous, 0, wallet);
    int price = definition.priceForLevel(previous + 1);
    if (wallet.balance(definition.currency()) < price)
      return result(
          TeamUpgradePurchaseCode.INSUFFICIENT_FUNDS, definition, previous, price, wallet);
    if (!wallet.pay(definition.currency(), price))
      return result(
          TeamUpgradePurchaseCode.TRANSACTION_FAILED, definition, previous, price, wallet);
    int level = team.upgrade(definition.type(), definition.maximumLevel());
    events.publish(
        new TeamUpgradePurchaseEvent(
            game.id(), playerId, team.id(), definition.type(), level, clock.instant()));
    return new TeamUpgradePurchaseResult(
        TeamUpgradePurchaseCode.SUCCESS,
        definition,
        previous,
        level,
        price,
        wallet.balance(definition.currency()));
  }

  private static TeamUpgradePurchaseResult result(
      TeamUpgradePurchaseCode code,
      TeamUpgradeDefinition definition,
      int level,
      int price,
      TeamUpgradeWallet wallet) {
    return new TeamUpgradePurchaseResult(
        code, definition, level, level, price, wallet.balance(definition.currency()));
  }
}
