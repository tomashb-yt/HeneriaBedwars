package fr.heneria.bedwars.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArenaBedDefinitionTest {
  @Test
  void teamStoresAndRestoresBothBedHalves() {
    ArenaTeamDefinition team =
        new ArenaTeamDefinition(
            new TeamId("red"),
            "Rouge",
            TeamColor.RED,
            1,
            1,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Map.of());
    ArenaBedDefinition bed =
        new ArenaBedDefinition(
            new ArenaBlockPosition("world", 1, 64, 2),
            new ArenaBlockPosition("world", 2, 64, 2),
            "east");

    ArenaTeamDefinition configured = team.withBedDefinition(Optional.of(bed));

    assertEquals(bed, configured.bedDefinition().orElseThrow());
    assertEquals(bed.foot().location(), configured.bedLocation().orElseThrow());
    assertTrue(configured.withBedDefinition(Optional.empty()).bedLocation().isEmpty());
  }

  @Test
  void bedHalvesMustBeAdjacentInTheSameWorld() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ArenaBedDefinition(
                new ArenaBlockPosition("world", 0, 64, 0),
                new ArenaBlockPosition("other", 1, 64, 0),
                "EAST"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ArenaBedDefinition(
                new ArenaBlockPosition("world", 0, 64, 0),
                new ArenaBlockPosition("world", 2, 64, 0),
                "EAST"));
  }
}
