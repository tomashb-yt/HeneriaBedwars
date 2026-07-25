package fr.heneria.zombie.core.instance;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable creation options for an instance.
 *
 * @param maximumPlayers map-defined capacity
 * @param owner optional private-game owner
 * @param access access policy
 */
public record GameInstanceOptions(int maximumPlayers, Optional<UUID> owner, InstanceAccess access) {

  /** Validates creation options. */
  public GameInstanceOptions {
    if (maximumPlayers <= 0) {
      throw new IllegalArgumentException("maximumPlayers must be positive");
    }
    owner = Objects.requireNonNull(owner, "owner");
    Objects.requireNonNull(access, "access");
  }

  /**
   * Creates public options without an owner.
   *
   * @param maximumPlayers capacity
   * @return public options
   */
  public static GameInstanceOptions publicGame(int maximumPlayers) {
    return new GameInstanceOptions(maximumPlayers, Optional.empty(), InstanceAccess.PUBLIC);
  }
}
