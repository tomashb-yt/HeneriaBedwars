package fr.heneria.zombie.core.editor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure structural and navigation validator for complete map definitions. */
public final class MapValidator {

  public ValidationReport validate(MapDefinition map) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    List<String> advice = new ArrayList<>();
    if (map.playerSpawn().isEmpty()) {
      errors.add("Spawn joueur manquant");
    }
    if (map.zones().isEmpty()) {
      errors.add("Aucune zone");
    }
    map.doors()
        .values()
        .forEach(
            door -> {
              if (!map.zones().containsKey(door.sourceZone())
                  || !map.zones().containsKey(door.targetZone())) {
                errors.add("Porte orpheline: " + door.id());
              }
            });
    map.zombieSpawns()
        .values()
        .forEach(
            spawn -> {
              if (!map.zones().containsKey(spawn.zone())) {
                errors.add("Spawn zombie orphelin: " + spawn.id());
              }
            });
    map.objects()
        .values()
        .forEach(
            object -> {
              if (!object.zone().isBlank() && !map.zones().containsKey(object.zone())) {
                errors.add("Objet orphelin: " + object.id());
              }
            });
    for (String zone : map.zones().keySet()) {
      boolean populated =
          map.zombieSpawns().values().stream().anyMatch(spawn -> spawn.zone().equals(zone))
              || map.objects().values().stream().anyMatch(object -> object.zone().equals(zone));
      if (!populated) {
        warnings.add("Zone vide: " + zone);
      }
    }
    requireObject(map, MapObjectType.MYSTERY_BOX, "Mystery Box manquante", warnings);
    requireObject(map, MapObjectType.PACK_A_PUNCH, "Pack-a-Punch manquant", warnings);
    boolean powerUsed =
        map.doors().values().stream().anyMatch(MapDefinition.Door::powerRequired)
            || map.objects().values().stream()
                .anyMatch(object -> object.type() == MapObjectType.POWER);
    if (powerUsed
        && map.objects().values().stream()
            .noneMatch(object -> object.type() == MapObjectType.POWER)) {
      errors.add("Power requis mais aucun interrupteur Power");
    }
    if (!map.zones().isEmpty() && !connected(map)) {
      errors.add("Navigation impossible entre toutes les zones");
    }
    if (map.zombieSpawns().isEmpty()) {
      advice.add("Ajouter au moins un spawn zombie");
    }
    return new ValidationReport(errors, warnings, advice);
  }

  private static void requireObject(
      MapDefinition map, MapObjectType type, String message, List<String> warnings) {
    if (map.objects().values().stream().noneMatch(object -> object.type() == type)) {
      warnings.add(message);
    }
  }

  private static boolean connected(MapDefinition map) {
    Map<String, Set<String>> graph = new HashMap<>();
    map.zones().keySet().forEach(zone -> graph.put(zone, new HashSet<>()));
    map.doors()
        .values()
        .forEach(
            door -> {
              if (graph.containsKey(door.sourceZone()) && graph.containsKey(door.targetZone())) {
                graph.get(door.sourceZone()).add(door.targetZone());
                graph.get(door.targetZone()).add(door.sourceZone());
              }
            });
    Set<String> visited = new HashSet<>();
    ArrayDeque<String> pending = new ArrayDeque<>();
    pending.add(map.zones().keySet().iterator().next());
    while (!pending.isEmpty()) {
      String current = pending.removeFirst();
      if (visited.add(current)) {
        pending.addAll(graph.get(current));
      }
    }
    return visited.size() == map.zones().size();
  }
}
