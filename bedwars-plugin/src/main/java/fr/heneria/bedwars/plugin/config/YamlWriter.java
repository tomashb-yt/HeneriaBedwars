package fr.heneria.bedwars.plugin.config;

import java.io.IOException;
import java.nio.file.Path;

/** Boundary for safe YAML persistence, injectable in migration tests. */
@FunctionalInterface
public interface YamlWriter {
  void write(Path target, String yaml) throws IOException;
}
