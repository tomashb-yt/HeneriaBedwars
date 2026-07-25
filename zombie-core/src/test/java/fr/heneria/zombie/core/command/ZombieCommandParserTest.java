package fr.heneria.zombie.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ZombieCommandParserTest {

  private final ZombieCommandParser parser = new ZombieCommandParser();

  @Test
  void parsesInformationLobbyAdministrationAndReload() {
    assertEquals(ZombieCommandAction.INFORMATION, parser.parse(new String[0]));
    assertEquals(ZombieCommandAction.HELP, parser.parse(new String[] {"HELP"}));
    assertEquals(ZombieCommandAction.RELOAD, parser.parse(new String[] {"reload"}));
    assertEquals(ZombieCommandAction.LOBBY, parser.parse(new String[] {"lobby"}));
    assertEquals(ZombieCommandAction.MAP_LIST, parser.parse(new String[] {"map", "list"}));
    assertEquals(
        ZombieCommandAction.MAP_PREVIEW, parser.parse(new String[] {"map", "preview", "crypt"}));
    assertEquals(ZombieCommandAction.MAP_LEAVE, parser.parse(new String[] {"map", "leave"}));
    assertEquals(
        ZombieCommandAction.INSTANCE_CREATE,
        parser.parse(new String[] {"instance", "create", "crypt"}));
    assertEquals(
        ZombieCommandAction.INSTANCE_LIST, parser.parse(new String[] {"instance", "list"}));
    assertEquals(
        ZombieCommandAction.INSTANCE_JOIN,
        parser.parse(new String[] {"instance", "join", "12345678"}));
    assertEquals(
        ZombieCommandAction.INSTANCE_LEAVE, parser.parse(new String[] {"instance", "leave"}));
    assertEquals(
        ZombieCommandAction.INSTANCE_STOP,
        parser.parse(new String[] {"instance", "stop", "12345678"}));
    assertEquals(
        ZombieCommandAction.INSTANCE_INFO,
        parser.parse(new String[] {"instance", "info", "12345678"}));
  }

  @Test
  void rejectsUnknownAndExtraArguments() {
    assertEquals(ZombieCommandAction.UNKNOWN, parser.parse(new String[] {"start"}));
    assertEquals(ZombieCommandAction.UNKNOWN, parser.parse(new String[] {"help", "extra"}));
    assertEquals(ZombieCommandAction.UNKNOWN, parser.parse(new String[] {"instance", "create"}));
  }
}
