package fr.heneria.bedwars.core.command;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Pure command behavior shared by Bukkit senders and unit tests. */
public final class BedWarsCommandLogic {
  public static final String PERMISSION = "heneriabedwars.admin";
  public static final String NO_PERMISSION =
      "Vous n'avez pas la permission d'utiliser cette commande.";

  /** Builds the response for the main command or its version subcommand. */
  public List<String> execute(
      boolean permitted, String label, String[] arguments, CommandDiagnostics diagnostics) {
    Objects.requireNonNull(label, "label");
    Objects.requireNonNull(arguments, "arguments");
    Objects.requireNonNull(diagnostics, "diagnostics");
    if (!permitted) {
      return List.of(NO_PERMISSION);
    }
    if (arguments.length == 1 && arguments[0].equalsIgnoreCase("version")) {
      return version(diagnostics);
    }
    return help(label, diagnostics.pluginName());
  }

  /** Returns permission-aware completions for the first argument. */
  public List<String> complete(boolean permitted, String[] arguments) {
    Objects.requireNonNull(arguments, "arguments");
    if (!permitted || arguments.length != 1) {
      return List.of();
    }
    String input = arguments[0].toLowerCase(Locale.ROOT);
    return "version".startsWith(input) ? List.of("version") : List.of();
  }

  private List<String> help(String label, String pluginName) {
    return List.of(pluginName, "/" + label + " version - Affiche les informations du plugin");
  }

  private List<String> version(CommandDiagnostics diagnostics) {
    return List.of(
        diagnostics.pluginName(),
        "Version : " + diagnostics.pluginVersion(),
        "Java : " + diagnostics.javaVersion(),
        "Serveur : " + diagnostics.serverVersion(),
        "État : " + diagnostics.pluginStatus(),
        "Services enregistrés : " + diagnostics.serviceCount());
  }
}
