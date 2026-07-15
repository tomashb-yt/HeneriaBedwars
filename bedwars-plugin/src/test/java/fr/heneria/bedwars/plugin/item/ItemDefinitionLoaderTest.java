package fr.heneria.bedwars.plugin.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationProblem;
import fr.heneria.bedwars.core.config.ProblemSeverity;
import fr.heneria.bedwars.core.item.ItemRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ItemDefinitionLoaderTest {
  @Test
  void loadsMinimalCompleteAndInheritedItemsWithDocumentedMerges() {
    Map<String, Object> values = base();
    values.put("items.base.material", "diamond_sword");
    values.put("items.base.amount", 1);
    values.put("items.base.name", "<aqua>Base");
    values.put("items.base.lore", List.of("base"));
    values.put("items.base.glow", true);
    values.put("items.base.unbreakable", true);
    values.put("items.base.custom-model-data", 1001);
    values.put("items.base.item-flags", List.of("HIDE_ATTRIBUTES", "HIDE_ATTRIBUTES"));
    values.put("items.base.enchantments.sharpness", 1);
    values.put("items.base.tags.category", "test");
    values.put("items.child.inherit", "base");
    values.put("items.child.lore", List.of("child"));
    values.put("items.child.enchantments.sharpness", 2);
    values.put("items.child.enchantments.unbreaking", 2);
    values.put("items.minimal.material", "STONE");
    List<ConfigurationProblem> problems = new ArrayList<>();

    ItemRegistry registry = load(values, problems);

    assertEquals(3, registry.size());
    assertEquals("DIAMOND_SWORD", registry.require("base").material());
    assertEquals(1001, registry.require("base").customModelData());
    assertTrue(registry.require("base").unbreakable());
    assertEquals("child", registry.require("child").lore().get(0).direct());
    assertEquals(Map.of("sharpness", 2, "unbreaking", 2), registry.require("child").enchantments());
    assertEquals(1, registry.require("minimal").amount());
    assertTrue(problems.stream().noneMatch(problem -> problem.severity() == ProblemSeverity.ERROR));
  }

  @Test
  void invalidVisualPropertiesApplySafeFallbacksAndWarnings() {
    Map<String, Object> values = base();
    values.put("items.invalid.material", "BARRIERE");
    values.put("items.invalid.amount", 0);
    values.put("items.invalid.item-flags", List.of("UNKNOWN_FLAG"));
    values.put("items.invalid.enchantments.unknown", 4);
    values.put("items.invalid.leather-color", "#FF0000");
    values.put("items.invalid.custom-model-data", -1);
    List<ConfigurationProblem> problems = new ArrayList<>();

    ItemRegistry registry = load(values, problems);

    assertEquals("BARRIER", registry.require("invalid").material());
    assertEquals(1, registry.require("invalid").amount());
    assertTrue(registry.require("invalid").itemFlags().isEmpty());
    assertTrue(registry.require("invalid").enchantments().isEmpty());
    assertEquals(null, registry.require("invalid").leatherColor());
    assertTrue(problems.stream().anyMatch(problem -> problem.message().equals("Unknown material")));
    assertTrue(
        problems.stream().allMatch(problem -> problem.severity() == ProblemSeverity.WARNING));
  }

  @Test
  void invalidFallbackIsCriticalAndCyclesRejectResolvedRegistry() {
    Map<String, Object> values = base();
    values.put("fallback-item.material", "UNKNOWN");
    values.put("items.a.inherit", "b");
    values.put("items.b.inherit", "a");
    List<ConfigurationProblem> problems = new ArrayList<>();

    ItemRegistry registry = load(values, problems);

    assertEquals("BARRIER", registry.fallback().material());
    assertEquals(0, registry.size());
    assertTrue(
        problems.stream().anyMatch(problem -> problem.severity() == ProblemSeverity.CRITICAL));
    assertTrue(problems.stream().anyMatch(problem -> problem.key().equals("items.inherit")));
    assertFalse(problems.isEmpty());
  }

  private static ItemRegistry load(
      Map<String, Object> values, List<ConfigurationProblem> problems) {
    return new ItemDefinitionLoader()
        .load(new ConfigurationDocument("ITEMS", "items.yml", 1, values), Map.of(), problems);
  }

  private static Map<String, Object> base() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("config-version", 1);
    values.put("fallback-item.material", "BARRIER");
    values.put("fallback-item.amount", 1);
    values.put("fallback-item.name", "Fallback");
    return values;
  }
}
