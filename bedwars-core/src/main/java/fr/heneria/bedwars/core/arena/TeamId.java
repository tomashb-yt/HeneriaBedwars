package fr.heneria.bedwars.core.arena;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable, filesystem-safe identifier of one persistent BedWars team. */
public record TeamId(String value) implements Comparable<TeamId> {
  private static final Pattern VALID = Pattern.compile("[a-z0-9_-]{2,24}");

  public TeamId {
    value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
    if (!VALID.matcher(value).matches())
      throw new IllegalArgumentException("Team id must match [a-z0-9_-]{2,24}");
  }

  @Override
  public int compareTo(TeamId other) {
    return value.compareTo(Objects.requireNonNull(other, "other").value);
  }
}
