package fr.heneria.zombie.core.weapon;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/** Deterministic weighted Mystery Box selector with blacklist and Wonder Weapon filtering. */
public final class MysteryBoxSelector {
  public Optional<WeaponDefinition> select(
      Collection<WeaponDefinition> definitions,
      Set<String> blacklist,
      boolean allowWonderWeapons,
      IntUnaryOperator random) {
    java.util.List<WeaponDefinition> eligible =
        definitions.stream()
            .filter(value -> value.mysteryWeight() > 0)
            .filter(value -> !blacklist.contains(value.id()))
            .filter(
                value ->
                    allowWonderWeapons
                        || value.category() != WeaponDefinition.WeaponCategory.WONDER_WEAPON)
            .sorted(Comparator.comparing(WeaponDefinition::id))
            .toList();
    int total = eligible.stream().mapToInt(WeaponDefinition::mysteryWeight).sum();
    if (total <= 0) {
      return Optional.empty();
    }
    int cursor = Math.floorMod(random.applyAsInt(total), total);
    for (WeaponDefinition definition : eligible) {
      cursor -= definition.mysteryWeight();
      if (cursor < 0) {
        return Optional.of(definition);
      }
    }
    return Optional.empty();
  }
}
