package fr.heneria.bedwars.core.game.upgrade;

import fr.heneria.bedwars.core.game.shop.ShopCurrency;
import java.util.List;
import java.util.Objects;

/** Immutable levels and prices captured from upgrades.yml. */
public record TeamUpgradeDefinition(
    TeamUpgradeType type,
    ShopCurrency currency,
    List<Integer> prices,
    String translationKey,
    int order) {
  public TeamUpgradeDefinition {
    type = Objects.requireNonNull(type, "type");
    currency = Objects.requireNonNull(currency, "currency");
    prices = List.copyOf(Objects.requireNonNull(prices, "prices"));
    translationKey = Objects.requireNonNull(translationKey, "translationKey").trim();
    if (prices.isEmpty() || prices.size() > 10 || prices.stream().anyMatch(price -> price < 1))
      throw new IllegalArgumentException("Upgrade prices are invalid");
    if (translationKey.isEmpty()) throw new IllegalArgumentException("translationKey is blank");
    if (order < 0) throw new IllegalArgumentException("order cannot be negative");
  }

  public int maximumLevel() {
    return prices.size();
  }

  public int priceForLevel(int level) {
    if (level < 1 || level > prices.size()) throw new IllegalArgumentException("Invalid level");
    return prices.get(level - 1);
  }
}
