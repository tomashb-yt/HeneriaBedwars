package fr.heneria.bedwars.core.map;

import java.util.List;

/** Metadata reload result; malformed files remain isolated and visible in diagnostics. */
public record MapTemplateLoadResult(List<MapTemplate> templates, List<String> failures) {
  public MapTemplateLoadResult {
    templates = List.copyOf(templates);
    failures = List.copyOf(failures);
  }
}
