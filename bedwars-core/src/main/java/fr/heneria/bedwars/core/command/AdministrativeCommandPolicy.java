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
  public static final String ARENA = "heneriabedwars.admin.arena";
  public static final String ARENA_CREATE = "heneriabedwars.admin.arena.create";
  public static final String ARENA_LIST = "heneriabedwars.admin.arena.list";
  public static final String ARENA_INFO = "heneriabedwars.admin.arena.info";
  public static final String ARENA_EDIT = "heneriabedwars.admin.arena.edit";
  public static final String ARENA_ENABLE = "heneriabedwars.admin.arena.enable";
  public static final String ARENA_DISABLE = "heneriabedwars.admin.arena.disable";
  public static final String ARENA_DELETE = "heneriabedwars.admin.arena.delete";
  public static final String ARENA_MENU = "heneriabedwars.admin.arena.menu";
  public static final String ARENA_TELEPORT = "heneriabedwars.admin.arena.teleport";
  public static final String SETUP = "heneriabedwars.admin.setup";

  public List<String> complete(
      Predicate<String> permitted, String[] arguments, List<String> locales) {
    return complete(permitted, arguments, locales, List.of());
  }

  public List<String> complete(
      Predicate<String> permitted,
      String[] arguments,
      List<String> locales,
      List<String> itemKeys) {
    return complete(permitted, arguments, locales, itemKeys, List.of(), List.of());
  }

  public List<String> complete(
      Predicate<String> permitted,
      String[] arguments,
      List<String> locales,
      List<String> itemKeys,
      List<String> arenaIds,
      List<String> worlds) {
    Objects.requireNonNull(permitted, "permitted");
    Objects.requireNonNull(arguments, "arguments");
    Objects.requireNonNull(locales, "locales");
    Objects.requireNonNull(itemKeys, "itemKeys");
    Objects.requireNonNull(arenaIds, "arenaIds");
    Objects.requireNonNull(worlds, "worlds");
    List<String> choices = new ArrayList<>();
    if (arguments.length == 1) {
      if (permitted.test(ADMIN)) choices.add("version");
      if (permitted.test(RELOAD)) choices.add("reload");
      if (permitted.test(CONFIG)) choices.add("config");
      if (permitted.test(LANGUAGE)) choices.add("language");
      if (permitted.test(GUI)) choices.add("gui");
      if (permitted.test(ITEM)) choices.add("item");
      if (permitted.test(ARENA) || permitted.test(ARENA_MENU)) choices.add("arena");
      if (permitted.test(SETUP)) choices.add("setup");
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
    } else if (arguments.length == 2
        && arguments[0].equalsIgnoreCase("arena")
        && permitted.test(ARENA)) {
      if (permitted.test(ARENA_CREATE)) choices.add("create");
      if (permitted.test(ARENA_LIST)) choices.add("list");
      if (permitted.test(ARENA_INFO)) choices.add("info");
      if (permitted.test(ARENA_MENU)) choices.add("menu");
      if (permitted.test(ARENA_EDIT))
        choices.addAll(
            List.of(
                "setworld", "setwaiting", "setspectator", "setplayers", "setteams", "validate"));
      if (permitted.test(ARENA_ENABLE)) choices.add("enable");
      if (permitted.test(ARENA_DISABLE)) choices.add("disable");
      if (permitted.test(ARENA_DELETE)) choices.add("delete");
    } else if (arguments.length == 3
        && arguments[0].equalsIgnoreCase("arena")
        && !arguments[1].equalsIgnoreCase("create")
        && !arguments[1].equalsIgnoreCase("list")
        && !arguments[1].equalsIgnoreCase("menu")
        && permitted.test(ARENA)) {
      choices.addAll(arenaIds);
    } else if (arguments.length == 4
        && arguments[0].equalsIgnoreCase("arena")
        && arguments[1].equalsIgnoreCase("setworld")
        && permitted.test(ARENA_EDIT)) {
      choices.addAll(worlds);
    }
    String input =
        arguments.length == 0 ? "" : arguments[arguments.length - 1].toLowerCase(Locale.ROOT);
    return choices.stream()
        .filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(input))
        .toList();
  }
}
