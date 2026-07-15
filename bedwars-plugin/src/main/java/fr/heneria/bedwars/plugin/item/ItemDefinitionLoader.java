package fr.heneria.bedwars.plugin.item;

import fr.heneria.bedwars.core.config.ConfigurationDocument;
import fr.heneria.bedwars.core.config.ConfigurationProblem;
import fr.heneria.bedwars.core.config.ProblemSeverity;
import fr.heneria.bedwars.core.config.TranslationBundle;
import fr.heneria.bedwars.core.item.HeadDefinition;
import fr.heneria.bedwars.core.item.ItemDefinition;
import fr.heneria.bedwars.core.item.ItemDefinitionTemplate;
import fr.heneria.bedwars.core.item.ItemInheritanceResolver;
import fr.heneria.bedwars.core.item.ItemKey;
import fr.heneria.bedwars.core.item.ItemRegistry;
import fr.heneria.bedwars.core.item.ItemResolutionException;
import fr.heneria.bedwars.core.item.ItemText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

/** Converts flattened items.yml data into a validated immutable item registry. */
public final class ItemDefinitionLoader {
  private static final int MAXIMUM_INHERITANCE_DEPTH = 16;
  private static final List<String> SUFFIXES =
      List.of(
          ".custom-model-data",
          ".required-placeholders",
          ".item-flags",
          ".leather-color",
          ".lore-keys",
          ".head.type",
          ".head.value",
          ".enchantments.",
          ".tags.",
          ".material",
          ".amount",
          ".name-key",
          ".name",
          ".lore",
          ".glow",
          ".unbreakable",
          ".inherit");
  private final MaterialResolver materials = new MaterialResolver();
  private final EnchantmentResolver enchantmentResolver = new EnchantmentResolver();
  private final ItemFlagResolver flags = new ItemFlagResolver();

  public ItemRegistry load(
      ConfigurationDocument document,
      Map<String, TranslationBundle> languages,
      List<ConfigurationProblem> problems) {
    ItemDefinition fallback = fallback(document, languages, problems);
    Map<ItemKey, ItemDefinitionTemplate> templates = new LinkedHashMap<>();
    for (String rawKey : discoverKeys(document)) {
      try {
        ItemKey key = ItemKey.of(rawKey);
        if (templates.containsKey(key)) {
          problem(
              problems,
              ProblemSeverity.ERROR,
              document,
              "items." + rawKey,
              rawKey,
              "unique lowercase key",
              "previous registry",
              "Duplicate item key after normalization");
          continue;
        }
        templates.put(
            key,
            template(document, "items." + rawKey, key, fallback.material(), languages, problems));
      } catch (IllegalArgumentException exception) {
        problem(
            problems,
            ProblemSeverity.ERROR,
            document,
            "items." + rawKey,
            rawKey,
            "lowercase dotted item key",
            "previous registry",
            exception.getMessage());
      }
    }
    if (templates.isEmpty()) {
      problem(
          problems,
          ProblemSeverity.ERROR,
          document,
          "items",
          "empty",
          "at least one item definition",
          "previous registry",
          "Missing item definitions");
    }
    Map<ItemKey, ItemDefinition> definitions = new LinkedHashMap<>();
    try {
      new ItemInheritanceResolver(MAXIMUM_INHERITANCE_DEPTH)
          .resolve(templates)
          .forEach(
              (key, value) ->
                  definitions.put(
                      key,
                      validateResolved(
                          value.resolve(fallback.material()),
                          fallback.material(),
                          document,
                          problems)));
    } catch (ItemResolutionException exception) {
      problem(
          problems,
          ProblemSeverity.ERROR,
          document,
          "items.inherit",
          exception.getMessage(),
          "known acyclic parent within depth " + MAXIMUM_INHERITANCE_DEPTH,
          "previous registry",
          "Unable to resolve item inheritance");
    }
    return new ItemRegistry(definitions, fallback);
  }

  private ItemDefinition fallback(
      ConfigurationDocument document,
      Map<String, TranslationBundle> languages,
      List<ConfigurationProblem> problems) {
    ItemKey key = ItemKey.of("system.fallback");
    Object configuredMaterial = document.value("fallback-item.material");
    if (!(configuredMaterial instanceof String configured)
        || materials.resolve(configured).isEmpty()) {
      problem(
          problems,
          ProblemSeverity.CRITICAL,
          document,
          "fallback-item.material",
          configuredMaterial,
          "valid non-air material",
          "BARRIER",
          "Fallback item itself is invalid");
    }
    ItemDefinition value =
        template(document, "fallback-item", key, "BARRIER", languages, problems).resolve("BARRIER");
    Material material = materials.resolve(value.material()).orElse(null);
    if (material == null) {
      problem(
          problems,
          ProblemSeverity.CRITICAL,
          document,
          "fallback-item.material",
          value.material(),
          "valid non-air material",
          "BARRIER",
          "Fallback item itself is invalid");
      value = copy(value, "BARRIER", 1, value.itemFlags(), value.enchantments(), null);
    }
    return validateResolved(value, "BARRIER", document, problems);
  }

  private ItemDefinitionTemplate template(
      ConfigurationDocument document,
      String path,
      ItemKey key,
      String fallbackMaterial,
      Map<String, TranslationBundle> languages,
      List<ConfigurationProblem> problems) {
    ItemDefinitionTemplate.Builder builder = ItemDefinitionTemplate.builder(key);
    Object parent = document.value(path + ".inherit");
    if (parent instanceof String text && !text.isBlank()) {
      try {
        builder.parent(ItemKey.of(text));
      } catch (IllegalArgumentException exception) {
        problem(
            problems,
            ProblemSeverity.ERROR,
            document,
            path + ".inherit",
            text,
            "valid item key",
            "previous registry",
            "Invalid parent item key");
      }
    } else if (parent != null) {
      invalidType(
          document, path + ".inherit", parent, "item key string", "previous registry", problems);
    }
    Object material = document.value(path + ".material");
    if (material != null) {
      if (material instanceof String text)
        builder.material(validateMaterial(text, fallbackMaterial, document, path, problems));
      else
        invalidType(document, path + ".material", material, "string", fallbackMaterial, problems);
    }
    Object amount = document.value(path + ".amount");
    if (amount != null) {
      if (amount instanceof Number number && number.intValue() >= 1)
        builder.amount(number.intValue());
      else if (amount instanceof Number) {
        invalidType(document, path + ".amount", amount, "positive integer", 1, problems);
        builder.amount(1);
      } else invalidType(document, path + ".amount", amount, "integer", 1, problems);
    }
    ItemText name = text(document, path + ".name", path + ".name-key", languages, problems);
    if (name != null) builder.name(name);
    Object lore = document.value(path + ".lore");
    Object loreKeys = document.value(path + ".lore-keys");
    if (lore != null && loreKeys != null) {
      problem(
          problems,
          ProblemSeverity.ERROR,
          document,
          path + ".lore",
          "lore and lore-keys",
          "one text source",
          "previous registry",
          "Conflicting lore sources");
    } else if (lore != null) {
      builder.lore(textList(lore, false, document, path + ".lore", languages, problems));
    } else if (loreKeys != null) {
      builder.lore(textList(loreKeys, true, document, path + ".lore-keys", languages, problems));
    }
    optionalBoolean(document, path + ".glow", problems).ifPresent(builder::glow);
    optionalBoolean(document, path + ".unbreakable", problems).ifPresent(builder::unbreakable);
    if (document.values().containsKey(path + ".custom-model-data")) {
      Object custom = document.value(path + ".custom-model-data");
      if (custom == null) builder.customModelData(null);
      else if (custom instanceof Number number && number.intValue() >= 0)
        builder.customModelData(number.intValue());
      else
        invalidType(
            document,
            path + ".custom-model-data",
            custom,
            "non-negative integer or null",
            null,
            problems);
    }
    Object flags = document.value(path + ".item-flags");
    if (flags != null) builder.itemFlags(validateFlags(flags, document, path, problems));
    Map<String, Integer> enchantments =
        prefixedIntegers(document, path + ".enchantments.", problems);
    if (!enchantments.isEmpty()) builder.enchantments(enchantments);
    Integer color = leatherColor(document, path, problems);
    if (color != null) builder.leatherColor(color);
    HeadDefinition head = head(document, path, problems);
    if (head != null) builder.head(head);
    Map<String, String> tags = prefixedStrings(document, path + ".tags.", problems);
    if (!tags.isEmpty()) builder.tags(tags);
    Object required = document.value(path + ".required-placeholders");
    if (required != null)
      builder.requiredPlaceholders(
          strings(required, document, path + ".required-placeholders", problems, false));
    return builder.build();
  }

  private ItemDefinition validateResolved(
      ItemDefinition value,
      String fallbackMaterial,
      ConfigurationDocument document,
      List<ConfigurationProblem> problems) {
    Material material = Material.matchMaterial(value.material());
    String resolvedMaterial = material == null ? fallbackMaterial : material.name();
    if (material == null) material = materials.resolve(fallbackMaterial).orElse(null);
    int amount = value.amount();
    int maximum = material == null ? 64 : material.getMaxStackSize();
    if (amount < 1 || amount > maximum) {
      problem(
          problems,
          ProblemSeverity.WARNING,
          document,
          "items." + value.key() + ".amount",
          amount,
          "1.." + maximum,
          1,
          "Invalid item amount");
      amount = 1;
    }
    Set<String> flags = new LinkedHashSet<>();
    for (String flag : value.itemFlags()) {
      try {
        ItemFlag resolved = this.flags.resolve(flag).orElse(null);
        if (resolved == null) throw new IllegalArgumentException(flag);
        flags.add(resolved.name());
      } catch (IllegalArgumentException exception) {
        problem(
            problems,
            ProblemSeverity.WARNING,
            document,
            "items." + value.key() + ".item-flags",
            flag,
            "known Bukkit ItemFlag",
            "ignored",
            "Unknown item flag");
      }
    }
    Map<String, Integer> enchantments = new LinkedHashMap<>();
    value
        .enchantments()
        .forEach(
            (name, level) -> {
              EnchantmentResolver.ResolvedEnchantment enchantment =
                  enchantmentResolver.resolve(name).orElse(null);
              if (enchantment == null) {
                problem(
                    problems,
                    ProblemSeverity.WARNING,
                    document,
                    "items." + value.key() + ".enchantments." + name,
                    name,
                    "known enchantment",
                    "ignored",
                    "Unknown enchantment");
              } else if (level < enchantment.minimum() || level > enchantment.maximum()) {
                problem(
                    problems,
                    ProblemSeverity.WARNING,
                    document,
                    "items." + value.key() + ".enchantments." + name,
                    level,
                    enchantment.minimum() + ".." + enchantment.maximum(),
                    "ignored",
                    "Invalid safe enchantment level");
              } else enchantments.put(enchantment.key(), level);
            });
    Integer color = value.leatherColor();
    if (color != null && (material == null || !material.name().startsWith("LEATHER_"))) {
      problem(
          problems,
          ProblemSeverity.WARNING,
          document,
          "items." + value.key() + ".leather-color",
          String.format("#%06X", color),
          "leather material",
          "ignored",
          "Leather color on incompatible material");
      color = null;
    }
    return copy(value, resolvedMaterial, amount, flags, enchantments, color);
  }

  private static ItemDefinition copy(
      ItemDefinition value,
      String material,
      int amount,
      Set<String> flags,
      Map<String, Integer> enchantments,
      Integer color) {
    return new ItemDefinition(
        value.key(),
        material,
        amount,
        value.name(),
        value.lore(),
        value.glow(),
        value.unbreakable(),
        value.customModelData(),
        flags,
        enchantments,
        color,
        value.head(),
        value.tags(),
        value.requiredPlaceholders());
  }

  private static Set<String> discoverKeys(ConfigurationDocument document) {
    Set<String> result = new LinkedHashSet<>();
    for (String path : document.values().keySet()) {
      if (!path.startsWith("items.")) continue;
      String relative = path.substring("items.".length());
      for (String suffix : SUFFIXES) {
        int index = relative.indexOf(suffix);
        if (index > 0) {
          result.add(relative.substring(0, index));
          break;
        }
      }
    }
    return result;
  }

  private String validateMaterial(
      String value,
      String fallback,
      ConfigurationDocument document,
      String path,
      List<ConfigurationProblem> problems) {
    Material material = materials.resolve(value).orElse(null);
    if (material != null) return material.name();
    problem(
        problems,
        ProblemSeverity.WARNING,
        document,
        path + ".material",
        value,
        "valid non-air material",
        fallback,
        "Unknown material");
    return fallback;
  }

  private static ItemText text(
      ConfigurationDocument document,
      String directPath,
      String keyPath,
      Map<String, TranslationBundle> languages,
      List<ConfigurationProblem> problems) {
    Object direct = document.value(directPath);
    Object key = document.value(keyPath);
    if (direct != null && key != null) {
      problem(
          problems,
          ProblemSeverity.ERROR,
          document,
          directPath,
          "name and name-key",
          "one text source",
          "previous registry",
          "Conflicting text sources");
      return null;
    }
    if (direct instanceof String value) return ItemText.direct(value);
    if (direct != null) invalidType(document, directPath, direct, "string", "", problems);
    if (key instanceof String value) {
      validateTranslation(value, languages, document, keyPath, problems);
      return ItemText.translated(value);
    }
    if (key != null) invalidType(document, keyPath, key, "string", "", problems);
    return null;
  }

  private static List<ItemText> textList(
      Object value,
      boolean translated,
      ConfigurationDocument document,
      String path,
      Map<String, TranslationBundle> languages,
      List<ConfigurationProblem> problems) {
    if (!(value instanceof List<?> list)) {
      invalidType(document, path, value, "list of strings", "empty list", problems);
      return List.of();
    }
    List<ItemText> result = new ArrayList<>();
    for (Object entry : list) {
      if (!(entry instanceof String text)) {
        invalidType(document, path, entry, "string", "ignored", problems);
        continue;
      }
      if (translated) validateTranslation(text, languages, document, path, problems);
      result.add(translated ? ItemText.translated(text) : ItemText.direct(text));
    }
    return result;
  }

  private static void validateTranslation(
      String key,
      Map<String, TranslationBundle> languages,
      ConfigurationDocument document,
      String path,
      List<ConfigurationProblem> problems) {
    for (Map.Entry<String, TranslationBundle> language : languages.entrySet())
      if (!language.getValue().messages().containsKey(key))
        problem(
            problems,
            ProblemSeverity.WARNING,
            document,
            path,
            key,
            "translation in " + language.getKey(),
            "empty text",
            "Missing item translation");
  }

  private static java.util.Optional<Boolean> optionalBoolean(
      ConfigurationDocument document, String path, List<ConfigurationProblem> problems) {
    Object value = document.value(path);
    if (value == null) return java.util.Optional.empty();
    if (value instanceof Boolean bool) return java.util.Optional.of(bool);
    invalidType(document, path, value, "boolean", false, problems);
    return java.util.Optional.empty();
  }

  private static Set<String> validateFlags(
      Object value,
      ConfigurationDocument document,
      String path,
      List<ConfigurationProblem> problems) {
    return strings(value, document, path + ".item-flags", problems, true);
  }

  private static Set<String> strings(
      Object value,
      ConfigurationDocument document,
      String path,
      List<ConfigurationProblem> problems,
      boolean uppercase) {
    if (!(value instanceof List<?> list)) {
      invalidType(document, path, value, "list of strings", "empty list", problems);
      return Set.of();
    }
    Set<String> result = new LinkedHashSet<>();
    for (Object entry : list)
      if (entry instanceof String text)
        result.add(uppercase ? text.toUpperCase(Locale.ROOT) : text);
      else invalidType(document, path, entry, "string", "ignored", problems);
    return result;
  }

  private static Map<String, Integer> prefixedIntegers(
      ConfigurationDocument document, String prefix, List<ConfigurationProblem> problems) {
    Map<String, Integer> result = new LinkedHashMap<>();
    document
        .values()
        .forEach(
            (path, value) -> {
              if (!path.startsWith(prefix)) return;
              String key = path.substring(prefix.length()).toLowerCase(Locale.ROOT);
              if (value instanceof Number number) result.put(key, number.intValue());
              else invalidType(document, path, value, "integer", "ignored", problems);
            });
    return result;
  }

  private static Map<String, String> prefixedStrings(
      ConfigurationDocument document, String prefix, List<ConfigurationProblem> problems) {
    Map<String, String> result = new LinkedHashMap<>();
    document
        .values()
        .forEach(
            (path, value) -> {
              if (!path.startsWith(prefix)) return;
              if (value instanceof String text) result.put(path.substring(prefix.length()), text);
              else invalidType(document, path, value, "string", "ignored", problems);
            });
    return result;
  }

  private static Integer leatherColor(
      ConfigurationDocument document, String path, List<ConfigurationProblem> problems) {
    Object value = document.value(path + ".leather-color");
    if (value instanceof String text && text.matches("#[0-9a-fA-F]{6}"))
      return Integer.parseInt(text.substring(1), 16);
    Object red = document.value(path + ".leather-color.red");
    Object green = document.value(path + ".leather-color.green");
    Object blue = document.value(path + ".leather-color.blue");
    if (red == null && green == null && blue == null && value == null) return null;
    if (red instanceof Number r
        && green instanceof Number g
        && blue instanceof Number b
        && inByte(r.intValue())
        && inByte(g.intValue())
        && inByte(b.intValue()))
      return Color.fromRGB(r.intValue(), g.intValue(), b.intValue()).asRGB();
    invalidType(
        document,
        path + ".leather-color",
        value,
        "#RRGGBB or RGB values 0..255",
        "ignored",
        problems);
    return null;
  }

  private static boolean inByte(int value) {
    return value >= 0 && value <= 255;
  }

  private static HeadDefinition head(
      ConfigurationDocument document, String path, List<ConfigurationProblem> problems) {
    Object type = document.value(path + ".head.type");
    Object value = document.value(path + ".head.value");
    if (type == null && value == null) return null;
    if (!(type instanceof String text)) {
      invalidType(
          document, path + ".head.type", type, "player or context-player", "ignored", problems);
      return null;
    }
    String normalized = text.toLowerCase(Locale.ROOT);
    if (normalized.equals("context-player"))
      return new HeadDefinition(HeadDefinition.Type.CONTEXT_PLAYER, "");
    if (normalized.equals("player") && value instanceof String owner && !owner.isBlank())
      return new HeadDefinition(HeadDefinition.Type.PLAYER, owner);
    problem(
        problems,
        ProblemSeverity.WARNING,
        document,
        path + ".head",
        text,
        "player with value or context-player",
        "ignored",
        "Invalid head definition");
    return null;
  }

  private static void invalidType(
      ConfigurationDocument document,
      String path,
      Object value,
      Object expected,
      Object fallback,
      List<ConfigurationProblem> problems) {
    problem(
        problems,
        ProblemSeverity.WARNING,
        document,
        path,
        value,
        expected,
        fallback,
        "Invalid item property type");
  }

  private static void problem(
      List<ConfigurationProblem> problems,
      ProblemSeverity severity,
      ConfigurationDocument document,
      String path,
      Object value,
      Object expected,
      Object fallback,
      String message) {
    problems.add(
        new ConfigurationProblem(
            severity,
            document.fileName(),
            path,
            String.valueOf(value),
            String.valueOf(expected),
            String.valueOf(fallback),
            message));
  }
}
