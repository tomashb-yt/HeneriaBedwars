package fr.heneria.bedwars.core.game.generator;

import fr.heneria.bedwars.api.game.GameState;
import fr.heneria.bedwars.core.game.GameId;
import fr.heneria.bedwars.core.game.GameInstance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** One bounded, fair coordinator for all generator deadlines; it owns no scheduler. */
public final class GameGeneratorService {
  private final int maxEmissionsPerTick;
  private int nextOffset;

  public GameGeneratorService(int maxEmissionsPerTick) {
    if (maxEmissionsPerTick < 1)
      throw new IllegalArgumentException("Generator emission budget must be positive");
    this.maxEmissionsPerTick = maxEmissionsPerTick;
  }

  public synchronized GeneratorTickReport tick(
      Collection<GameInstance> games, Instant now, GeneratorCapacityView capacityView) {
    Objects.requireNonNull(games, "games");
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(capacityView, "capacityView");

    List<Slot> slots = activeSlots(games);
    if (slots.isEmpty()) {
      nextOffset = 0;
      return GeneratorTickReport.empty();
    }

    int start = Math.floorMod(nextOffset, slots.size());
    int visited = 0;
    int blocked = 0;
    int lastIndex = start;
    boolean truncated = false;
    List<GeneratorEmission> emissions = new ArrayList<>();
    Set<GameId> activeGames = new HashSet<>();
    slots.forEach(slot -> activeGames.add(slot.gameId()));

    for (int scanned = 0; scanned < slots.size(); scanned++) {
      int index = (start + scanned) % slots.size();
      Slot slot = slots.get(index);
      lastIndex = index;
      visited++;
      RuntimeGenerator.GeneratorPollResult result =
          slot.generator()
              .poll(
                  slot.gameId(),
                  now,
                  () -> capacityView.nearbyAmount(slot.gameId(), slot.generator().definition()));
      if (result.status() == RuntimeGenerator.Status.CAPACITY_REACHED) blocked++;
      result.emission().ifPresent(emissions::add);
      if (emissions.size() >= maxEmissionsPerTick && scanned + 1 < slots.size()) {
        truncated = true;
        break;
      }
    }

    nextOffset = (lastIndex + 1) % slots.size();
    return new GeneratorTickReport(emissions, activeGames.size(), visited, blocked, truncated);
  }

  private static List<Slot> activeSlots(Collection<GameInstance> games) {
    List<Slot> slots = new ArrayList<>();
    games.stream()
        .filter(game -> game.state() == GameState.PLAYING && game.world().isPresent())
        .sorted(Comparator.comparing(game -> game.id().toString()))
        .forEach(
            game ->
                game.generators().stream()
                    .sorted(Comparator.comparing(generator -> generator.definition().id()))
                    .forEach(generator -> slots.add(new Slot(game.id(), generator))));
    return List.copyOf(slots);
  }

  private record Slot(GameId gameId, RuntimeGenerator generator) {}
}
