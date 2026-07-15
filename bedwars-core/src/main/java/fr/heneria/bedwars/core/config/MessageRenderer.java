package fr.heneria.bedwars.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Spigot-safe renderer for the supported MiniMessage subset and legacy color codes. */
public final class MessageRenderer {
  private static final Pattern HEX_TAG = Pattern.compile("<#[0-9a-fA-F]{6}>");
  private static final Pattern LEGACY_HEX = Pattern.compile("&#([0-9a-fA-F]{6})");
  private static final Pattern LEGACY = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
  private static final Map<String, String> TAGS = tags();

  public String render(String template, PlaceholderContext context) {
    String formatted = formatColors(template == null ? "" : template);
    for (Map.Entry<String, String> entry : context.values().entrySet()) {
      formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return formatted;
  }

  private String formatColors(String input) {
    String output = input;
    for (Map.Entry<String, String> entry : TAGS.entrySet()) {
      output = output.replace(entry.getKey(), entry.getValue());
    }
    Matcher tags = HEX_TAG.matcher(output);
    StringBuffer result = new StringBuffer();
    while (tags.find()) {
      tags.appendReplacement(result, Matcher.quoteReplacement(hex(tags.group().substring(2, 8))));
    }
    tags.appendTail(result);
    Matcher legacyHex = LEGACY_HEX.matcher(result.toString());
    result = new StringBuffer();
    while (legacyHex.find()) {
      legacyHex.appendReplacement(result, Matcher.quoteReplacement(hex(legacyHex.group(1))));
    }
    legacyHex.appendTail(result);
    return LEGACY.matcher(result.toString()).replaceAll("§$1");
  }

  private static String hex(String value) {
    StringBuilder result = new StringBuilder("§x");
    for (char character : value.toCharArray()) result.append('§').append(character);
    return result.toString();
  }

  private static Map<String, String> tags() {
    Map<String, String> tags = new LinkedHashMap<>();
    tags.put("<black>", "§0");
    tags.put("<dark_blue>", "§1");
    tags.put("<dark_green>", "§2");
    tags.put("<dark_aqua>", "§3");
    tags.put("<dark_red>", "§4");
    tags.put("<dark_purple>", "§5");
    tags.put("<gold>", "§6");
    tags.put("<gray>", "§7");
    tags.put("<dark_gray>", "§8");
    tags.put("<blue>", "§9");
    tags.put("<green>", "§a");
    tags.put("<aqua>", "§b");
    tags.put("<red>", "§c");
    tags.put("<light_purple>", "§d");
    tags.put("<yellow>", "§e");
    tags.put("<white>", "§f");
    tags.put("<bold>", "§l");
    tags.put("<italic>", "§o");
    tags.put("<underlined>", "§n");
    tags.put("<reset>", "§r");
    tags.put("</bold>", "§r");
    tags.put("</italic>", "§r");
    tags.put("</underlined>", "§r");
    return Map.copyOf(tags);
  }
}
