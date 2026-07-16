package fr.heneria.bedwars.core.gui;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Active input state with an absolute expiry; it contains no Bukkit player reference. */
public record TextInputSession(UUID playerId, TextInputRequest request, Instant expiresAt) {
  public TextInputSession {
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }
}
