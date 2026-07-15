package fr.heneria.bedwars.core.item;

import fr.heneria.bedwars.core.config.ConfigurationProblem;
import java.util.List;

/**
 * Result of an item-aware transactional configuration reload.
 *
 * @param successful whether the complete snapshot was activated
 * @param loadedItems active item count after the attempt
 * @param problems immutable diagnostics produced by the attempt
 */
public record ItemReloadResult(
    boolean successful, int loadedItems, List<ConfigurationProblem> problems) {
  public ItemReloadResult {
    problems = List.copyOf(problems);
  }
}
