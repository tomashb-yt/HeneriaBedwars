package fr.heneria.zombie.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ZombieCommandParserTest {

  private final ZombieCommandParser parser = new ZombieCommandParser();

  @Test
  void parsesInformationHelpAndReload() {
    assertEquals(ZombieCommandAction.INFORMATION, parser.parse(new String[0]));
    assertEquals(ZombieCommandAction.HELP, parser.parse(new String[] {"HELP"}));
    assertEquals(ZombieCommandAction.RELOAD, parser.parse(new String[] {"reload"}));
  }

  @Test
  void rejectsUnknownAndExtraArguments() {
    assertEquals(ZombieCommandAction.UNKNOWN, parser.parse(new String[] {"start"}));
    assertEquals(ZombieCommandAction.UNKNOWN, parser.parse(new String[] {"help", "extra"}));
  }
}
