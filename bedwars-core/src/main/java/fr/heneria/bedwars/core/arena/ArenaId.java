package fr.heneria.bedwars.core.arena;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable, filename-safe identifier for an arena. */
public record ArenaId(String value) implements Comparable<ArenaId> {
  private static final Pattern VALID = Pattern.compile("[a-z0-9_-]{2,32}");

  public ArenaId {
    Objects.requireNonNull(value, "value");
    if (!VALID.matcher(value).matches()) {
      throw new IllegalArgumentException("Arena id must match [a-z0-9_-]{2,32}");
    }
  }

  public static ArenaId parse(String value) {
    return new ArenaId(Objects.requireNonNull(value, "value").toLowerCase(Locale.ROOT));
  }

  @Override
  public int compareTo(ArenaId other) {
    return value.compareTo(other.value);
  }

  @Override
  public String toString() {
    return value;
  }
}
