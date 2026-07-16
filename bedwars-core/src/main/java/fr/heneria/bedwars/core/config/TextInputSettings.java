package fr.heneria.bedwars.core.config;

import java.time.Duration;
import java.util.Set;

/** Bounded chat-input configuration shared by all future administrative editors. */
public record TextInputSettings(Duration timeout, Set<String> cancelKeywords) {
  public TextInputSettings {
    cancelKeywords = Set.copyOf(cancelKeywords);
  }
}
