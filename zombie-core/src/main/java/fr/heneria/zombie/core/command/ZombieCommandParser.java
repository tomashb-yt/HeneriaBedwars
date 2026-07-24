package fr.heneria.zombie.core.command;

import java.util.Locale;
import java.util.Objects;

/** Pure parser for the initial {@code /zombie} command surface. */
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
    if (arguments.length != 1) {
      return ZombieCommandAction.UNKNOWN;
    }
    return switch (arguments[0].toLowerCase(Locale.ROOT)) {
      case "help" -> ZombieCommandAction.HELP;
      case "reload" -> ZombieCommandAction.RELOAD;
      default -> ZombieCommandAction.UNKNOWN;
    };
  }
}
