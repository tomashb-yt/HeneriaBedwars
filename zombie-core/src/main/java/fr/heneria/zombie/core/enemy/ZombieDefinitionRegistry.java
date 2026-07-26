package fr.heneria.zombie.core.enemy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Read-only registry atomically replaceable by its owner after complete validation. */
public final class ZombieDefinitionRegistry {
  private final Map<String, ZombieDefinition> definitions;

  public ZombieDefinitionRegistry(Collection<ZombieDefinition> definitions) {
    LinkedHashMap<String, ZombieDefinition> indexed = new LinkedHashMap<>();
    for (ZombieDefinition definition : definitions) {
      ZombieDefinition previous = indexed.putIfAbsent(definition.id(), definition);
      if (previous != null) {
        throw new IllegalArgumentException("Duplicate zombie type id: " + definition.id());
      }
      java.util.List<String> issues = definition.validate();
      if (!issues.isEmpty()) {
        throw new IllegalArgumentException(definition.id() + ": " + String.join("; ", issues));
      }
    }
    if (indexed.isEmpty()) {
      throw new IllegalArgumentException("At least one zombie definition is required");
    }
    this.definitions = Map.copyOf(indexed);
  }

  public Optional<ZombieDefinition> find(String id) {
    return Optional.ofNullable(definitions.get(id));
  }

  public Collection<ZombieDefinition> all() {
    return definitions.values();
  }

  public int size() {
    return definitions.size();
  }
}
