package fr.heneria.bedwars.core.config;

import fr.heneria.bedwars.core.item.ItemRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complete immutable runtime configuration activated as one unit. */
public record ConfigurationSnapshot(
    PluginSettings plugin,
    GameplaySettings gameplay,
    LobbySettings lobby,
    StorageSettings storage,
    WorldManagerSettings worlds,
    MenuSettings menus,
    ItemRegistry items,
    Map<ConfigurationId, ConfigurationDocument> documents,
    Map<String, TranslationBundle> languages,
    List<ConfigurationProblem> problems,
    Instant loadedAt) {
  public ConfigurationSnapshot {
    documents = Map.copyOf(documents);
    languages = Map.copyOf(languages);
    problems = List.copyOf(problems);
  }

  public TranslationBundle activeLanguage() {
    return languages.get(plugin.locale());
  }

  public int warningCount() {
    return (int)
        problems.stream().filter(problem -> problem.severity() == ProblemSeverity.WARNING).count();
  }

  public Set<String> availableLocales() {
    return languages.keySet();
  }
}
