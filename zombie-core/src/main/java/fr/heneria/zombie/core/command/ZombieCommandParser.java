package fr.heneria.zombie.core.command;

import java.util.Locale;
import java.util.Objects;

/** Pure parser for the {@code /zombie} command surface. */
public final class ZombieCommandParser {

  /**
   * Resolves command arguments without depending on Bukkit.
   *
   * @param arguments command arguments
   * @return resolved action
   */
  public ZombieCommandAction parse(String[] arguments) {
    Objects.requireNonNull(arguments, "arguments");
    if (arguments.length == 0) {
      return ZombieCommandAction.INFORMATION;
    }
    String root = arguments[0].toLowerCase(Locale.ROOT);
    if (arguments.length == 1) {
      return switch (root) {
        case "help" -> ZombieCommandAction.HELP;
        case "reload" -> ZombieCommandAction.RELOAD;
        case "lobby" -> ZombieCommandAction.LOBBY;
        case "admin" -> ZombieCommandAction.ADMIN;
        default -> ZombieCommandAction.UNKNOWN;
      };
    }
    if (root.equals("map")) {
      String operation = arguments[1].toLowerCase(Locale.ROOT);
      return switch (operation) {
        case "list" ->
            arguments.length == 2 ? ZombieCommandAction.MAP_LIST : ZombieCommandAction.UNKNOWN;
        case "preview" ->
            arguments.length == 3 ? ZombieCommandAction.MAP_PREVIEW : ZombieCommandAction.UNKNOWN;
        case "leave" ->
            arguments.length == 2 ? ZombieCommandAction.MAP_LEAVE : ZombieCommandAction.UNKNOWN;
        default -> ZombieCommandAction.UNKNOWN;
      };
    }
    if (!root.equals("instance")) {
      return ZombieCommandAction.UNKNOWN;
    }
    String operation = arguments[1].toLowerCase(Locale.ROOT);
    return switch (operation) {
      case "create" ->
          arguments.length == 3 ? ZombieCommandAction.INSTANCE_CREATE : ZombieCommandAction.UNKNOWN;
      case "list" ->
          arguments.length == 2 ? ZombieCommandAction.INSTANCE_LIST : ZombieCommandAction.UNKNOWN;
      case "join" ->
          arguments.length == 3 ? ZombieCommandAction.INSTANCE_JOIN : ZombieCommandAction.UNKNOWN;
      case "leave" ->
          arguments.length == 2 ? ZombieCommandAction.INSTANCE_LEAVE : ZombieCommandAction.UNKNOWN;
      case "stop" ->
          arguments.length == 3 ? ZombieCommandAction.INSTANCE_STOP : ZombieCommandAction.UNKNOWN;
      case "info" ->
          arguments.length == 3 ? ZombieCommandAction.INSTANCE_INFO : ZombieCommandAction.UNKNOWN;
      default -> ZombieCommandAction.UNKNOWN;
    };
  }
}
