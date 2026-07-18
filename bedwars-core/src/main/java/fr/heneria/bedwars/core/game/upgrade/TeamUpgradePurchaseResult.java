package fr.heneria.bedwars.core.game.upgrade;

import java.util.Objects;

public record TeamUpgradePurchaseResult(
    TeamUpgradePurchaseCode code,
    TeamUpgradeDefinition definition,
    int previousLevel,
    int level,
    int price,
    int balance) {
  public TeamUpgradePurchaseResult {
    code = Objects.requireNonNull(code, "code");
    definition = Objects.requireNonNull(definition, "definition");
  }

  public boolean successful() {
    return code == TeamUpgradePurchaseCode.SUCCESS;
  }
}
