package fr.heneria.zombie.core.weapon;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable weapon-definition registry activated only after complete validation. */
public final class WeaponRegistry {
  private final Map<String, WeaponDefinition> definitions;

  public WeaponRegistry(Collection<WeaponDefinition> definitions) {
    LinkedHashMap<String, WeaponDefinition> indexed = new LinkedHashMap<>();
    for (WeaponDefinition definition : definitions) {
      if (indexed.putIfAbsent(definition.id(), definition) != null) {
        throw new IllegalArgumentException("Duplicate weapon id: " + definition.id());
      }
      if (!definition.validate().isEmpty()) {
        throw new IllegalArgumentException(
            definition.id() + ": " + String.join("; ", definition.validate()));
      }
    }
    if (indexed.isEmpty()) {
      throw new IllegalArgumentException("At least one weapon definition is required");
    }
    this.definitions = Map.copyOf(indexed);
  }

  public Optional<WeaponDefinition> find(String id) {
    return Optional.ofNullable(definitions.get(id));
  }

  public Collection<WeaponDefinition> all() {
    return definitions.values();
  }

  public int size() {
    return definitions.size();
  }
}
