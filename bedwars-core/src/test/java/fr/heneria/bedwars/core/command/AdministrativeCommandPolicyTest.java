package fr.heneria.bedwars.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdministrativeCommandPolicyTest {
  private final AdministrativeCommandPolicy policy = new AdministrativeCommandPolicy();

  @Test
  void filtersEachTopLevelSubcommandByItsOwnPermission() {
    Set<String> permissions =
        Set.of(AdministrativeCommandPolicy.RELOAD, AdministrativeCommandPolicy.LANGUAGE);
    assertEquals(
        List.of("reload", "language"),
        policy.complete(permissions::contains, new String[] {""}, List.of()));
  }

  @Test
  void publicPlayerOnlySeesGameAndPublicActions() {
    Set<String> permissions =
        Set.of(AdministrativeCommandPolicy.GAME_JOIN, AdministrativeCommandPolicy.GAME_LEAVE);
    assertEquals(
        List.of("game"), policy.complete(permissions::contains, new String[] {""}, List.of()));
  }

  @Test
  void exposesStatisticsWithoutAdministrativePermission() {
    assertEquals(
        List.of("stats"),
        policy.complete(
            permission -> permission.equals(AdministrativeCommandPolicy.STATISTICS_VIEW),
            new String[] {""},
            List.of()));
  }

  @Test
  void exposesLeaderboardWithoutAdministrativePermission() {
    assertEquals(
        List.of("top"),
        policy.complete(
            permission -> permission.equals(AdministrativeCommandPolicy.STATISTICS_LEADERBOARD),
            new String[] {""},
            List.of()));
  }

  @Test
  void exposesGuiOnlyWithGuiPermission() {
    assertEquals(
        List.of("gui"),
        policy.complete(
            permission -> permission.equals(AdministrativeCommandPolicy.GUI),
            new String[] {""},
            List.of()));
  }

  @Test
  void completesItemActionsAndKnownKeysBySpecificPermission() {
    Set<String> permissions =
        Set.of(
            AdministrativeCommandPolicy.ITEM,
            AdministrativeCommandPolicy.ITEM_GIVE,
            AdministrativeCommandPolicy.ITEM_PREVIEW);
    assertEquals(
        List.of("list", "give", "preview"),
        policy.complete(
            permissions::contains, new String[] {"item", ""}, List.of(), List.of("gui.close")));
    assertEquals(
        List.of("gui.close"),
        policy.complete(
            permissions::contains,
            new String[] {"item", "give", "gui"},
            List.of(),
            List.of("gui.close", "demo.info")));
  }

  @Test
  void exposesLanguageSetAndKnownLocalesOnlyWithLanguagePermission() {
    assertEquals(
        List.of("set"),
        policy.complete(
            permission -> permission.equals(AdministrativeCommandPolicy.LANGUAGE),
            new String[] {"language", ""},
            List.of("en_US", "fr_FR")));
    assertEquals(
        List.of("en_US", "fr_FR"),
        policy.complete(
            permission -> permission.equals(AdministrativeCommandPolicy.LANGUAGE),
            new String[] {"language", "set", ""},
            List.of("en_US", "fr_FR")));
    assertEquals(
        List.of(),
        policy.complete(
            permission -> false, new String[] {"language", "set", ""}, List.of("en_US", "fr_FR")));
  }

  @Test
  void completesArenaActionsAccordingToSpecificPermissions() {
    Set<String> permissions =
        Set.of(
            AdministrativeCommandPolicy.ARENA,
            AdministrativeCommandPolicy.ARENA_LIST,
            AdministrativeCommandPolicy.ARENA_EDIT,
            AdministrativeCommandPolicy.ARENA_DELETE);
    assertEquals(
        List.of(
            "list",
            "setworld",
            "setmap",
            "setwaiting",
            "setspectator",
            "setplayers",
            "setteams",
            "validate",
            "delete"),
        policy.complete(
            permissions::contains,
            new String[] {"arena", ""},
            List.of(),
            List.of(),
            List.of("alpha"),
            List.of("world")));
  }

  @Test
  void completesArenaIdsAndWorlds() {
    assertEquals(
        List.of("alpha"),
        policy.complete(
            permission -> true,
            new String[] {"arena", "info", "a"},
            List.of(),
            List.of(),
            List.of("alpha", "beta"),
            List.of("world")));
    assertEquals(
        List.of("world_nether"),
        policy.complete(
            permission -> true,
            new String[] {"arena", "setworld", "alpha", "world_n"},
            List.of(),
            List.of(),
            List.of("alpha"),
            List.of("world", "world_nether")));
  }

  @Test
  void completesMapCommandsTypesIdsAndArenaAssociation() {
    Set<String> permissions =
        Set.of(
            AdministrativeCommandPolicy.MAP,
            AdministrativeCommandPolicy.MAP_MENU,
            AdministrativeCommandPolicy.MAP_CREATE,
            AdministrativeCommandPolicy.MAP_LOAD,
            AdministrativeCommandPolicy.ARENA,
            AdministrativeCommandPolicy.ARENA_EDIT);
    assertTrue(
        policy
            .complete(
                permissions::contains,
                new String[] {"map", ""},
                List.of(),
                List.of(),
                List.of("arena"),
                List.of("world"),
                List.of("desert"))
            .containsAll(List.of("menu", "create", "load")));
    assertEquals(
        List.of("lobby", "bedwars", "generic"),
        policy.complete(
            permissions::contains,
            new String[] {"map", "create", "desert", ""},
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of("desert")));
    assertEquals(
        List.of("desert"),
        policy.complete(
            permissions::contains,
            new String[] {"arena", "setmap", "arena", ""},
            List.of(),
            List.of(),
            List.of("arena"),
            List.of(),
            List.of("desert")));
  }
}
