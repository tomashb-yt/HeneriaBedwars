package fr.heneria.zombie.core.powerup;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Per-game timed bonus registry driven by the existing grouped game tick. */
public final class PowerUpService {
  private final PowerUpRegistry registry;
  private final Clock clock;
  private final Consumer<PowerUpEvent> events;
  private final Map<UUID, EnumMap<PowerUpType, PowerUpInstance>> active = new ConcurrentHashMap<>();

  public PowerUpService(PowerUpRegistry registry, Clock clock, Consumer<PowerUpEvent> events) {
    this.registry = java.util.Objects.requireNonNull(registry, "registry");
    this.clock = java.util.Objects.requireNonNull(clock, "clock");
    this.events = java.util.Objects.requireNonNull(events, "events");
  }

  public synchronized ActivationResult activate(
      UUID gameId, PowerUpType type, UUID collectorId, String source) {
    PowerUpDefinition definition = registry.find(type).orElse(null);
    if (definition == null) {
      return new ActivationResult(false, Optional.empty(), "Unknown power-up");
    }
    EnumMap<PowerUpType, PowerUpInstance> effects =
        active.computeIfAbsent(gameId, ignored -> new EnumMap<>(PowerUpType.class));
    Instant now = clock.instant();
    PowerUpInstance current = effects.get(type);
    if (current != null && current.expiresAt().isAfter(now)) {
      if (definition.stackPolicy() == StackPolicy.REJECT) {
        return new ActivationResult(false, Optional.of(current), "Already active");
      }
      Instant expiration =
          switch (definition.stackPolicy()) {
            case REFRESH_DURATION, REPLACE -> now.plus(definition.duration());
            case EXTEND_DURATION, MULTIPLY ->
                current
                        .expiresAt()
                        .plus(definition.duration())
                        .isAfter(now.plus(definition.maximumDuration()))
                    ? now.plus(definition.maximumDuration())
                    : current.expiresAt().plus(definition.duration());
            case KEEP_STRONGEST, REJECT -> current.expiresAt();
          };
      PowerUpInstance replaced =
          new PowerUpInstance(
              UUID.randomUUID(),
              gameId,
              definition,
              Optional.ofNullable(collectorId),
              now,
              expiration,
              source);
      effects.put(type, replaced);
      events.accept(new PowerUpEvent(PowerUpEvent.Type.ACTIVATED, gameId, now, replaced));
      return new ActivationResult(true, Optional.of(replaced), "");
    }
    PowerUpInstance created =
        new PowerUpInstance(
            UUID.randomUUID(),
            gameId,
            definition,
            Optional.ofNullable(collectorId),
            now,
            now.plus(definition.duration()),
            source);
    if (!definition.duration().isZero()) {
      effects.put(type, created);
    }
    events.accept(new PowerUpEvent(PowerUpEvent.Type.ACTIVATED, gameId, now, created));
    return new ActivationResult(true, Optional.of(created), "");
  }

  public synchronized List<PowerUpInstance> tick() {
    Instant now = clock.instant();
    ArrayList<PowerUpInstance> expired = new ArrayList<>();
    active.forEach(
        (gameId, effects) ->
            effects
                .values()
                .removeIf(
                    effect -> {
                      if (effect.expiresAt().isAfter(now)) {
                        return false;
                      }
                      expired.add(effect);
                      events.accept(
                          new PowerUpEvent(PowerUpEvent.Type.EXPIRED, gameId, now, effect));
                      return true;
                    }));
    active.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    return List.copyOf(expired);
  }

  public synchronized boolean active(UUID gameId, PowerUpType type) {
    PowerUpInstance effect =
        active.getOrDefault(gameId, new EnumMap<>(PowerUpType.class)).get(type);
    return effect != null && effect.expiresAt().isAfter(clock.instant());
  }

  public synchronized double pointMultiplier(UUID gameId) {
    PowerUpInstance effect =
        active
            .getOrDefault(gameId, new EnumMap<>(PowerUpType.class))
            .get(PowerUpType.DOUBLE_POINTS);
    return effect == null || !effect.expiresAt().isAfter(clock.instant())
        ? 1
        : effect.definition().pointMultiplier();
  }

  public synchronized List<PowerUpInstance> active(UUID gameId) {
    return List.copyOf(active.getOrDefault(gameId, new EnumMap<>(PowerUpType.class)).values());
  }

  public synchronized void clear(UUID gameId) {
    active.remove(gameId);
  }

  public record ActivationResult(
      boolean activated, Optional<PowerUpInstance> instance, String failureReason) {}
}
