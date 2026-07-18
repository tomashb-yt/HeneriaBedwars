package fr.heneria.bedwars.core.game.shop;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Safe configuration identifier for a shop offer. */
public record ShopOfferId(String value) implements Comparable<ShopOfferId> {
  private static final Pattern SAFE = Pattern.compile("[a-z0-9_-]{2,32}");

  public ShopOfferId {
    value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
    if (!SAFE.matcher(value).matches())
      throw new IllegalArgumentException("Invalid shop offer id: " + value);
  }

  @Override
  public int compareTo(ShopOfferId other) {
    return value.compareTo(other.value);
  }
}
