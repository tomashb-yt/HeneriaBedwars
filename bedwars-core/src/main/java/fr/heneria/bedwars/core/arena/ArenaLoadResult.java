package fr.heneria.bedwars.core.arena;

import java.util.List;
import java.util.Map;

/** Repository scan result; known failed ids let reload preserve their active definitions. */
public record ArenaLoadResult(
    List<ArenaDefinition> definitions,
    Map<ArenaId, String> failedKnownIds,
    List<String> unreadableFiles) {
  public ArenaLoadResult {
    definitions = List.copyOf(definitions);
    failedKnownIds = Map.copyOf(failedKnownIds);
    unreadableFiles = List.copyOf(unreadableFiles);
  }
}
