package fr.heneria.zombie.plugin.message;

import fr.heneria.zombie.plugin.config.ConfigurationManager;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/** Renders configurable MiniMessage templates with safely escaped placeholders. */
public final class MessageService {

  private static final Pattern TAG_PATTERN = Pattern.compile("(?<!\\\\)<([a-zA-Z0-9_|-]+)>");
  private final ConfigurationManager configurations;
  private final MiniMessage miniMessage;

  /**
   * Creates the message service.
   *
   * @param configurations active configuration provider
   */
  public MessageService(ConfigurationManager configurations) {
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.miniMessage = MiniMessage.miniMessage();
  }

  /**
   * Renders one configured message.
   *
   * @param key dotted message key
   * @param placeholders alternating placeholder names and values
   * @return rendered component
   */
  public Component render(String key, String... placeholders) {
    if (placeholders.length % 2 != 0) {
      throw new IllegalArgumentException("Placeholders must be supplied as name/value pairs");
    }
    TagResolver.Builder resolver = TagResolver.builder();
    Set<String> suppliedNames = new java.util.HashSet<>();
    for (int index = 0; index < placeholders.length; index += 2) {
      suppliedNames.add(placeholders[index]);
      resolver.resolver(Placeholder.unparsed(placeholders[index], placeholders[index + 1]));
    }
    String template =
        configurations
            .current()
            .messages()
            .getOrDefault(key, "<red>Message manquant: " + key + "</red>");
    return miniMessage.deserialize(
        escapeLiteralArgumentTags(template, suppliedNames), resolver.build());
  }

  private static String escapeLiteralArgumentTags(String template, Set<String> suppliedNames) {
    Matcher matcher = TAG_PATTERN.matcher(template);
    StringBuilder escaped = new StringBuilder(template.length());
    while (matcher.find()) {
      String tag = matcher.group(1);
      boolean placeholder = suppliedNames.contains(tag);
      boolean commandArgument =
          tag.contains("|") || tag.equals("id") || tag.equals("map") || tag.equals("mapId");
      matcher.appendReplacement(
          escaped,
          Matcher.quoteReplacement(
              !placeholder && commandArgument ? "\\<" + tag + ">" : matcher.group()));
    }
    matcher.appendTail(escaped);
    return escaped.toString();
  }
}
