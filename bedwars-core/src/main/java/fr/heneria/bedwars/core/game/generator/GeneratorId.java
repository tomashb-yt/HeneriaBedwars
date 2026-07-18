package fr.heneria.bedwars.core.game.generator;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Stable identifier of one generator inside an arena and its runtime clone. */
public record GeneratorId(String value) implements Comparable<GeneratorId> {
  private static final Pattern VALID = Pattern.compile("[a-z0-9][a-z0-9_-]{1,47}");

  public GeneratorId {
    value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
    if (!VALID.matcher(value).matches())
      throw new IllegalArgumentException("Generator id must match [a-z0-9][a-z0-9_-]{1,47}");
  }

  @Override
  public int compareTo(GeneratorId other) {
    return value.compareTo(other.value);
  }

  @Override
  public String toString() {
    return value;
  }
}
