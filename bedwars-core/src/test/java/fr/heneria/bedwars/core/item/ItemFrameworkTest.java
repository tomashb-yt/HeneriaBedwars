package fr.heneria.bedwars.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ItemFrameworkTest {
  @Test
  void keysNormalizeCaseAndRejectUnsafeFormats() {
    assertEquals("gui.close", ItemKey.of("GUI.Close").value());
    assertThrows(IllegalArgumentException.class, () -> ItemKey.of(""));
    assertThrows(IllegalArgumentException.class, () -> ItemKey.of("gui close"));
    assertThrows(IllegalArgumentException.class, () -> ItemKey.of(".close"));
  }

  @Test
  void resolvedDefinitionIsDeeplyImmutable() {
    ItemDefinition definition = definition("gui.close");
    assertThrows(
        UnsupportedOperationException.class, () -> definition.lore().add(ItemText.direct("x")));
    assertThrows(UnsupportedOperationException.class, () -> definition.tags().put("x", "y"));
    assertThrows(UnsupportedOperationException.class, () -> definition.enchantments().put("x", 1));
  }

  @Test
  void inheritanceOverridesLoreAndMergesFlagsEnchantmentsAndTags() {
    ItemDefinitionTemplate parent =
        ItemDefinitionTemplate.builder(ItemKey.of("item.parent"))
            .material("STONE")
            .amount(2)
            .lore(List.of(ItemText.direct("parent")))
            .itemFlags(Set.of("HIDE_ATTRIBUTES"))
            .enchantments(Map.of("sharpness", 1, "unbreaking", 1))
            .tags(Map.of("category", "base"))
            .build();
    ItemDefinitionTemplate child =
        ItemDefinitionTemplate.builder(ItemKey.of("item.child"))
            .parent(ItemKey.of("item.parent"))
            .amount(1)
            .lore(List.of(ItemText.direct("child")))
            .itemFlags(Set.of("HIDE_ENCHANTS"))
            .enchantments(Map.of("sharpness", 2))
            .tags(Map.of("action", "close"))
            .build();
    Map<ItemKey, ItemDefinitionTemplate> resolved =
        new ItemInheritanceResolver(8).resolve(Map.of(parent.key(), parent, child.key(), child));
    ItemDefinition value = resolved.get(child.key()).resolve("BARRIER");
    assertEquals("STONE", value.material());
    assertEquals(1, value.amount());
    assertEquals("child", value.lore().get(0).direct());
    assertEquals(Set.of("HIDE_ATTRIBUTES", "HIDE_ENCHANTS"), value.itemFlags());
    assertEquals(Map.of("sharpness", 2, "unbreaking", 1), value.enchantments());
    assertEquals(Map.of("category", "base", "action", "close"), value.tags());
    assertEquals(2, parent.resolve("BARRIER").amount());
  }

  @Test
  void inheritanceSupportsDepthAndRejectsUnknownParent() {
    ItemDefinitionTemplate a = template("a", null);
    ItemDefinitionTemplate b = template("b", "a");
    ItemDefinitionTemplate c = template("c", "b");
    assertEquals(
        "STONE",
        new ItemInheritanceResolver(4)
            .resolve(Map.of(a.key(), a, b.key(), b, c.key(), c))
            .get(c.key())
            .resolve("BARRIER")
            .material());
    ItemDefinitionTemplate missing = template("missing", "unknown");
    assertThrows(
        ItemResolutionException.class,
        () -> new ItemInheritanceResolver(4).resolve(Map.of(missing.key(), missing)));
  }

  @Test
  void inheritanceRejectsDirectIndirectCyclesAndDepthLimit() {
    ItemDefinitionTemplate direct = template("direct", "direct");
    assertThrows(
        ItemResolutionException.class,
        () -> new ItemInheritanceResolver(8).resolve(Map.of(direct.key(), direct)));
    ItemDefinitionTemplate a = template("a", "b");
    ItemDefinitionTemplate b = template("b", "c");
    ItemDefinitionTemplate c = template("c", "a");
    assertThrows(
        ItemResolutionException.class,
        () -> new ItemInheritanceResolver(8).resolve(Map.of(a.key(), a, b.key(), b, c.key(), c)));
    ItemDefinitionTemplate root = template("root", null);
    ItemDefinitionTemplate child = template("child", "root");
    assertThrows(
        ItemResolutionException.class,
        () -> new ItemInheritanceResolver(1).resolve(Map.of(root.key(), root, child.key(), child)));
  }

  @Test
  void contextAcceptsPlayerLocaleNumbersBooleansAndSafePlaceholderValues() {
    UUID player = UUID.randomUUID();
    ItemContext context =
        ItemContext.builder()
            .player(player, "<red>Alex")
            .locale("fr_FR")
            .menu("demo")
            .page(2)
            .placeholder("amount", 5)
            .placeholder("enabled", true)
            .build();
    assertEquals(player, context.playerId().orElseThrow());
    assertEquals("<red>Alex", context.placeholderContext().values().get("player"));
    assertEquals("5", context.placeholders().get("amount"));
    assertEquals("true", context.placeholders().get("enabled"));
  }

  @Test
  void registryFindRequireAndKeysNeverReturnNull() {
    ItemDefinition fallback = definition("system.fallback");
    ItemDefinition close = definition("gui.close");
    ItemRegistry registry = new ItemRegistry(Map.of(close.key(), close), fallback);
    assertTrue(registry.find("GUI.CLOSE").isPresent());
    assertFalse(registry.find("bad key").isPresent());
    assertEquals(close, registry.require("gui.close"));
    assertThrows(UnknownItemException.class, () -> registry.require("gui.missing"));
    assertEquals(Set.of("gui.close"), registry.keys());
  }

  private static ItemDefinitionTemplate template(String key, String parent) {
    ItemDefinitionTemplate.Builder builder =
        ItemDefinitionTemplate.builder(ItemKey.of(key)).material("STONE");
    if (parent != null) builder.parent(ItemKey.of(parent));
    return builder.build();
  }

  private static ItemDefinition definition(String key) {
    return new ItemDefinition(
        ItemKey.of(key),
        "STONE",
        1,
        ItemText.direct("name"),
        List.of(ItemText.direct("lore")),
        false,
        false,
        null,
        Set.of(),
        new LinkedHashMap<>(),
        null,
        null,
        Map.of("category", "test"),
        Set.of());
  }
}
