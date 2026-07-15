package fr.heneria.bedwars.core.config;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable messages for one locale with a built-in diagnostic fallback. */
public record TranslationBundle(String locale, int version, Map<String, String> messages) {
  public TranslationBundle {
    Objects.requireNonNull(locale, "locale");
    messages = Map.copyOf(messages);
  }

  public String message(TranslationKey key) {
    return message(key.path());
  }

  public String message(String key) {
    return messages.getOrDefault(key, "<red>Missing translation: " + key);
  }

  public Set<String> keys() {
    return messages.keySet();
  }
}
