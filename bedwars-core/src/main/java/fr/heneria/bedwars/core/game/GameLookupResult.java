package fr.heneria.bedwars.core.game;

import java.util.Optional;

/** Explicit lookup result so short identifiers can never select an ambiguous instance. */
public record GameLookupResult(GameLookupStatus status, Optional<GameInstance> instance) {
  public GameLookupResult {
    instance = instance == null ? Optional.empty() : instance;
    if ((status == GameLookupStatus.FOUND) != instance.isPresent())
      throw new IllegalArgumentException("Lookup status and instance disagree");
  }

  public static GameLookupResult found(GameInstance instance) {
    return new GameLookupResult(GameLookupStatus.FOUND, Optional.of(instance));
  }

  public static GameLookupResult missing() {
    return new GameLookupResult(GameLookupStatus.NOT_FOUND, Optional.empty());
  }

  public static GameLookupResult ambiguous() {
    return new GameLookupResult(GameLookupStatus.AMBIGUOUS, Optional.empty());
  }
}
