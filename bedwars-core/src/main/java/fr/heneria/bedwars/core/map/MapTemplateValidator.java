package fr.heneria.bedwars.core.map;

import java.util.ArrayList;
import java.util.List;

/** Pure metadata validation performed before a template enters the active registry. */
public final class MapTemplateValidator {
  public List<String> validate(MapTemplate template) {
    List<String> problems = new ArrayList<>();
    if (!template.folderName().equals(template.id().value()))
      problems.add("folder-name must equal map id");
    if (!template.spawn().world().equals(template.worldName()))
      problems.add("spawn world does not match managed world");
    if (template.worldName().contains("/") || template.worldName().contains("\\"))
      problems.add("unsafe Bukkit world name");
    if (template.displayName().length() > 64) problems.add("display name is longer than 64");
    return List.copyOf(problems);
  }
}
