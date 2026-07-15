package fr.heneria.bedwars.core.item;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable, normalized identifier used to address an item definition.
 *
 * @param value lowercase dotted value after normalization
 */
public record ItemKey(String value) implements Comparable<ItemKey> {
  private static final Pattern FORMAT = Pattern.compile("[a-z0-9][a-z0-9._-]*");

  public ItemKey {
    Objects.requireNonNull(value, "value");
    value = value.toLowerCase(Locale.ROOT);
    if (!FORMAT.matcher(value).matches())
      throw new IllegalArgumentException("Invalid item key: " + value);
  }

  public static ItemKey of(String value) {
    return new ItemKey(value);
  }

  @Override
  public int compareTo(ItemKey other) {
    return value.compareTo(other.value);
  }

  @Override
  public String toString() {
    return value;
  }
}
