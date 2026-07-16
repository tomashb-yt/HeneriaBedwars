package fr.heneria.bedwars.core.arena;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe copy-on-write view of all administrative arena definitions. */
public final class ArenaRegistry {
  private final AtomicReference<Map<ArenaId, ArenaDefinition>> active =
      new AtomicReference<>(Map.of());

  public Optional<ArenaDefinition> find(ArenaId id) {
    return Optional.ofNullable(active.get().get(id));
  }

  public Collection<ArenaDefinition> all() {
    return active.get().values();
  }

  public int size() {
    return active.get().size();
  }

  public void replace(Collection<ArenaDefinition> definitions) {
    Map<ArenaId, ArenaDefinition> next = new LinkedHashMap<>();
    definitions.stream()
        .sorted((a, b) -> a.id().compareTo(b.id()))
        .forEach(
            arena -> {
              if (next.put(arena.id(), arena) != null)
                throw new IllegalArgumentException("Duplicate arena " + arena.id());
            });
    active.set(Map.copyOf(next));
  }

  public Map<ArenaId, ArenaDefinition> snapshot() {
    return active.get();
  }
}
