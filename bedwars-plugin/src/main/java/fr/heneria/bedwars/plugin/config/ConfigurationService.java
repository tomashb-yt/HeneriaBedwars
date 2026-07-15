package fr.heneria.bedwars.plugin.config;

import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationId;
import fr.heneria.bedwars.core.config.ConfigurationProblem;
import fr.heneria.bedwars.core.config.ConfigurationRegistry;
import fr.heneria.bedwars.core.config.ConfigurationReloadResult;
import fr.heneria.bedwars.core.config.ConfigurationSnapshot;
import fr.heneria.bedwars.core.config.LanguageService;
import fr.heneria.bedwars.core.config.MessageRenderer;
import fr.heneria.bedwars.core.config.ProblemSeverity;
import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/** Owns installation, validation, and transactional activation of runtime configuration. */
public final class ConfigurationService {
  private final Path dataDirectory;
  private final DefaultConfigurationInstaller installer;
  private final ConfigurationLoader loader = new ConfigurationLoader();
  private final ConfigurationSnapshotFactory snapshotFactory;
  private final ConfigurationRegistry registry = new ConfigurationRegistry();
  private final SafeYamlWriter writer = new SafeYamlWriter();
  private final ProjectLogger logger;
  private final AtomicReference<ConfigurationSnapshot> active = new AtomicReference<>();
  private final LanguageService languageService =
      new LanguageService(this::snapshot, new MessageRenderer());
  private volatile ConfigurationReloadResult lastResult =
      new ConfigurationReloadResult(false, 0, 0, 0, List.of());

  public ConfigurationService(
      Path dataDirectory,
      DefaultConfigurationInstaller.ResourceProvider resources,
      ProjectLogger logger,
      Clock clock) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    this.logger = Objects.requireNonNull(logger, "logger");
    this.installer = new DefaultConfigurationInstaller(dataDirectory, resources, logger);
    this.snapshotFactory = new ConfigurationSnapshotFactory(clock);
    for (ConfigurationId id : ConfigurationId.values()) {
      registry.register(id);
    }
  }

  /** Installs missing defaults and requires the first candidate to be fully valid. */
  public void initialize() throws IOException {
    installer.installMissing();
    ConfigurationReloadResult result = reloadAll();
    if (!result.successful())
      throw new IOException("Initial configuration is invalid; see preceding diagnostics");
  }

  /** Loads and validates every file before swapping the active snapshot once. */
  public synchronized ConfigurationReloadResult reloadAll() {
    long started = System.nanoTime();
    Candidate candidate = loadCandidate();
    int warnings = count(candidate.problems(), ProblemSeverity.WARNING);
    int errors =
        count(candidate.problems(), ProblemSeverity.ERROR)
            + count(candidate.problems(), ProblemSeverity.CRITICAL);
    boolean successful = candidate.snapshot() != null && errors == 0;
    if (successful) {
      registry.activate(candidate.snapshot().documents());
      active.set(candidate.snapshot());
      logger.setDebug(candidate.snapshot().plugin().debug());
      logger.info(
          "Loaded "
              + candidate.loadedFiles()
              + " configuration files with "
              + warnings
              + " warning(s).");
      if (candidate.snapshot().plugin().debug()) {
        logger.debug(
            "Configuration load took " + ((System.nanoTime() - started) / 1_000_000.0) + " ms.");
        logger.debug(
            "Detected languages: " + String.join(", ", candidate.snapshot().availableLocales()));
      }
    } else {
      registry.recordError(
          candidate.problems().isEmpty()
              ? "Unknown reload failure"
              : candidate.problems().get(0).toLogMessage());
      logger.warning("Configuration reload rejected; the previous snapshot remains active.");
    }
    candidate
        .problems()
        .forEach(
            problem -> {
              if (problem.severity() == ProblemSeverity.ERROR
                  || problem.severity() == ProblemSeverity.CRITICAL) {
                logger.warning(problem.toLogMessage());
              } else if (problem.severity() == ProblemSeverity.WARNING
                  && active.get() != null
                  && active.get().plugin().debug()) {
                logger.debug(problem.toLogMessage());
              }
            });
    lastResult =
        new ConfigurationReloadResult(
            successful, candidate.loadedFiles(), warnings, errors, candidate.problems());
    return lastResult;
  }

  /** Reloads safely; cross-file validation means the complete candidate is rebuilt. */
  public ConfigurationReloadResult reload(ConfigurationId ignored) {
    Objects.requireNonNull(ignored, "id");
    return reloadAll();
  }

  /** Persists and activates a known locale, rolling the file back if validation fails. */
  public synchronized ConfigurationReloadResult setLanguage(String locale) {
    if (!snapshot().availableLocales().contains(locale)) {
      ConfigurationProblem problem =
          new ConfigurationProblem(
              ProblemSeverity.ERROR,
              "config.yml",
              "plugin.language",
              locale,
              "one of " + availableLocales(),
              "unchanged",
              "Unknown locale");
      return new ConfigurationReloadResult(false, 0, 0, 1, List.of(problem));
    }
    Path config = dataDirectory.resolve("config.yml");
    try {
      String original = Files.readString(config, StandardCharsets.UTF_8);
      YamlConfiguration yaml = new YamlConfiguration();
      yaml.loadFromString(original);
      yaml.set("plugin.language", locale);
      writer.write(config, yaml.saveToString());
      ConfigurationReloadResult result = reloadAll();
      if (!result.successful()) writer.write(config, original);
      return result;
    } catch (IOException | InvalidConfigurationException exception) {
      logger.error("Unable to update plugin.language safely", exception);
      ConfigurationProblem problem =
          new ConfigurationProblem(
              ProblemSeverity.ERROR,
              "config.yml",
              "plugin.language",
              locale,
              "writable valid YAML",
              "unchanged",
              "Safe write failed");
      return new ConfigurationReloadResult(false, 0, 0, 1, List.of(problem));
    }
  }

  public ConfigurationSnapshot snapshot() {
    ConfigurationSnapshot snapshot = active.get();
    if (snapshot == null) throw new IllegalStateException("Configuration has not been initialized");
    return snapshot;
  }

  public LanguageService language() {
    return languageService;
  }

  public ConfigurationRegistry registry() {
    return registry;
  }

  public List<String> availableLocales() {
    return snapshot().availableLocales().stream().sorted().toList();
  }

  public ConfigurationReloadResult lastResult() {
    return lastResult;
  }

  private Candidate loadCandidate() {
    Map<ConfigurationId, ConfigurationDocument> documents = new EnumMap<>(ConfigurationId.class);
    Map<String, ConfigurationDocument> languages = new LinkedHashMap<>();
    List<ConfigurationProblem> problems = new ArrayList<>();
    int loaded = 0;
    for (ConfigurationId id : ConfigurationId.values()) {
      try {
        documents.put(id, loader.load(id.name(), dataDirectory.resolve(id.fileName()).toFile()));
        loaded++;
      } catch (IOException | InvalidConfigurationException exception) {
        problems.add(loadFailure(id.fileName(), exception));
      }
    }
    Path languageDirectory = dataDirectory.resolve("languages");
    try (Stream<Path> files = Files.list(languageDirectory)) {
      for (Path file :
          files
              .filter(path -> path.getFileName().toString().endsWith(".yml"))
              .sorted(Comparator.comparing(Path::toString))
              .toList()) {
        String fileName = file.getFileName().toString();
        String locale = fileName.substring(0, fileName.length() - 4);
        try {
          languages.put(locale, loader.load("LANGUAGE_" + locale, file.toFile()));
          loaded++;
        } catch (IOException | InvalidConfigurationException exception) {
          problems.add(loadFailure("languages/" + fileName, exception));
        }
      }
    } catch (IOException exception) {
      problems.add(loadFailure("languages", exception));
    }
    if (documents.size() != ConfigurationId.values().length || languages.isEmpty()) {
      return new Candidate(null, loaded, List.copyOf(problems));
    }
    try {
      ConfigurationSnapshot snapshot = snapshotFactory.create(documents, languages, problems);
      return new Candidate(snapshot, loaded, snapshot.problems());
    } catch (RuntimeException exception) {
      problems.add(
          new ConfigurationProblem(
              ProblemSeverity.CRITICAL,
              "configuration",
              "snapshot",
              exception.getClass().getSimpleName(),
              "valid typed values",
              "previous snapshot",
              exception.getMessage()));
      return new Candidate(null, loaded, List.copyOf(problems));
    }
  }

  private static ConfigurationProblem loadFailure(String file, Exception exception) {
    return new ConfigurationProblem(
        ProblemSeverity.ERROR,
        file,
        "yaml",
        exception.getClass().getSimpleName(),
        "valid readable YAML",
        "previous snapshot",
        "Unable to load file");
  }

  private static int count(List<ConfigurationProblem> problems, ProblemSeverity severity) {
    return (int) problems.stream().filter(problem -> problem.severity() == severity).count();
  }

  private record Candidate(
      ConfigurationSnapshot snapshot, int loadedFiles, List<ConfigurationProblem> problems) {}
}
