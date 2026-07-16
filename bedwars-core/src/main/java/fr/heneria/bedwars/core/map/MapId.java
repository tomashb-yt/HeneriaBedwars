package fr.heneria.bedwars.core.map;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable path-safe identifier for a persistent map template, never a game instance. */
public record MapId(String value) implements Comparable<MapId> {
  private static final Pattern VALID = Pattern.compile("[a-z0-9_-]{2,32}");

  public MapId {
    value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
    if (!VALID.matcher(value).matches())
      throw new IllegalArgumentException("Map id must match [a-z0-9_-]{2,32}");
  }

  public static MapId parse(String value) {
    return new MapId(value);
  }

  @Override
  public int compareTo(MapId other) {
    return value.compareTo(other.value);
  }

  @Override
  public String toString() {
    return value;
  }
}
