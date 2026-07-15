package fr.heneria.bedwars.core.item;

import java.util.Objects;
import java.util.Optional;

/** Text supplied directly or indirectly through a language-catalog key. */
public record ItemText(String direct, String translationKey) {
  public ItemText {
    if (direct != null && translationKey != null)
      throw new IllegalArgumentException("Item text cannot be direct and translated");
  }

  public static ItemText direct(String value) {
    return new ItemText(Objects.requireNonNullElse(value, ""), null);
  }

  public static ItemText translated(String key) {
    if (key == null || key.isBlank()) throw new IllegalArgumentException("Blank translation key");
    return new ItemText(null, key);
  }

  public Optional<String> directValue() {
    return Optional.ofNullable(direct);
  }

  public Optional<String> translation() {
    return Optional.ofNullable(translationKey);
  }
}
