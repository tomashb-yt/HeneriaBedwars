package fr.heneria.bedwars.core.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/** Pure permission-aware tab-completion policy for administrative commands. */
public final class AdministrativeCommandPolicy {
  public static final String ADMIN = "heneriabedwars.admin";
  public static final String RELOAD = "heneriabedwars.admin.reload";
  public static final String CONFIG = "heneriabedwars.admin.config";
  public static final String LANGUAGE = "heneriabedwars.admin.language";
  public static final String GUI = "heneriabedwars.admin.gui";
  public static final String ITEM = "heneriabedwars.admin.item";
  public static final String ITEM_GIVE = "heneriabedwars.admin.item.give";
  public static final String ITEM_PREVIEW = "heneriabedwars.admin.item.preview";

  public List<String> complete(
      Predicate<String> permitted, String[] arguments, List<String> locales) {
    return complete(permitted, arguments, locales, List.of());
  }

  public List<String> complete(
      Predicate<String> permitted,
      String[] arguments,
      List<String> locales,
      List<String> itemKeys) {
    Objects.requireNonNull(permitted, "permitted");
    Objects.requireNonNull(arguments, "arguments");
    Objects.requireNonNull(locales, "locales");
    Objects.requireNonNull(itemKeys, "itemKeys");
    List<String> choices = new ArrayList<>();
    if (arguments.length == 1) {
      if (permitted.test(ADMIN)) choices.add("version");
      if (permitted.test(RELOAD)) choices.add("reload");
      if (permitted.test(CONFIG)) choices.add("config");
      if (permitted.test(LANGUAGE)) choices.add("language");
      if (permitted.test(GUI)) choices.add("gui");
      if (permitted.test(ITEM)) choices.add("item");
    } else if (arguments.length == 2
        && arguments[0].equalsIgnoreCase("language")
        && permitted.test(LANGUAGE)) {
      choices.add("set");
    } else if (arguments.length == 3
        && arguments[0].equalsIgnoreCase("language")
        && arguments[1].equalsIgnoreCase("set")
        && permitted.test(LANGUAGE)) {
      choices.addAll(locales);
    } else if (arguments.length == 2
        && arguments[0].equalsIgnoreCase("item")
        && permitted.test(ITEM)) {
      choices.add("list");
      if (permitted.test(ITEM_GIVE)) choices.add("give");
      if (permitted.test(ITEM_PREVIEW)) choices.add("preview");
    } else if (arguments.length == 3
        && arguments[0].equalsIgnoreCase("item")
        && arguments[1].equalsIgnoreCase("give")
        && permitted.test(ITEM_GIVE)) {
      choices.addAll(itemKeys.stream().limit(100).toList());
    }
    String input =
        arguments.length == 0 ? "" : arguments[arguments.length - 1].toLowerCase(Locale.ROOT);
    return choices.stream()
        .filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(input))
        .toList();
  }
}
