package fr.heneria.bedwars.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BedWarsCommandLogicTest {
  private final BedWarsCommandLogic logic = new BedWarsCommandLogic();
  private final CommandDiagnostics diagnostics =
      new CommandDiagnostics(
          "HeneriaBedWars", "0.1.0-SNAPSHOT", "21.0.8", "Spigot 1.21", "actif", 2);

  @Test
  void displaysMainHelpWithoutArguments() {
    assertEquals(
        List.of("HeneriaBedWars", "/bedwars version - Affiche les informations du plugin"),
        logic.execute(true, "bedwars", new String[0], diagnostics));
  }

  @Test
  void displaysVersionDiagnostics() {
    List<String> messages = logic.execute(true, "bedwars", new String[] {"version"}, diagnostics);
    assertEquals("HeneriaBedWars", messages.get(0));
    assertTrue(messages.contains("Version : 0.1.0-SNAPSHOT"));
    assertTrue(messages.contains("Serveur : Spigot 1.21"));
    assertTrue(messages.contains("Services enregistrés : 2"));
  }

  @Test
  void refusesSenderWithoutPermission() {
    assertEquals(
        List.of(BedWarsCommandLogic.NO_PERMISSION),
        logic.execute(false, "bedwars", new String[0], diagnostics));
  }

  @Test
  void handlesConsoleLikeAnyPermittedSender() {
    assertEquals(
        "HeneriaBedWars",
        logic.execute(true, "bedwars", new String[] {"version"}, diagnostics).get(0));
  }

  @Test
  void completesVersionOnlyForPermittedSenders() {
    assertEquals(List.of("version"), logic.complete(true, new String[] {"ver"}));
    assertTrue(logic.complete(false, new String[] {"ver"}).isEmpty());
  }
}
