package fr.heneria.bedwars.core.statistics;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Last verified Minecraft name associated with a player UUID. */
public record PlayerIdentity(UUID playerId, String currentName) {
  public PlayerIdentity {
    Objects.requireNonNull(playerId, "playerId");
    currentName = Objects.requireNonNull(currentName, "currentName").trim();
    if (currentName.isEmpty() || currentName.length() > 32)
      throw new IllegalArgumentException("currentName must contain 1 to 32 characters");
    if (!currentName.matches("[A-Za-z0-9_.-]+"))
      throw new IllegalArgumentException("currentName contains unsupported characters");
  }

  public String normalizedName() {
    return normalize(currentName);
  }

  public static String normalize(String value) {
    return Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
  }
}
