package fr.heneria.bedwars.core.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ArenaMapTemplateValidationTest {
  private static final Instant NOW = Instant.parse("2026-07-16T20:00:00Z");

  @Test
  void missingMapTemplateIsBlocking() {
    ArenaValidator validator =
        new ArenaValidator(world -> true, id -> ArenaMapTemplateStatus.VALID);
    var codes = codes(validator, ArenaDefinition.draft(ArenaId.parse("arena"), NOW));
    assertTrue(codes.contains("MAP_TEMPLATE_MISSING"));
    assertTrue(codes.stream().noneMatch(code -> code.equals("missing-world")));
  }

  @Test
  void unknownWrongTypeAndErrorMapHaveDistinctDiagnostics() {
    ArenaDefinition arena =
        ArenaDefinition.draft(ArenaId.parse("arena"), NOW)
            .withTemplate("desert", "hbw_template_desert", ArenaStatus.INVALID, NOW);
    assertEquals("MAP_TEMPLATE_NOT_FOUND", mapCode(arena, ArenaMapTemplateStatus.NOT_FOUND));
    assertEquals("MAP_TEMPLATE_INVALID_TYPE", mapCode(arena, ArenaMapTemplateStatus.INVALID_TYPE));
    assertEquals("MAP_TEMPLATE_ERROR", mapCode(arena, ArenaMapTemplateStatus.ERROR));
  }

  @Test
  void validBedWarsMapDoesNotAddMapDiagnostic() {
    ArenaDefinition arena =
        ArenaDefinition.draft(ArenaId.parse("arena"), NOW)
            .withTemplate("desert", "hbw_template_desert", ArenaStatus.INVALID, NOW);
    assertTrue(
        codes(new ArenaValidator(world -> true, id -> ArenaMapTemplateStatus.VALID), arena).stream()
            .noneMatch(code -> code.startsWith("MAP_TEMPLATE_")));
  }

  @Test
  void validTemplateDoesNotRequireItsConstructionWorldToStayLoaded() {
    ArenaDefinition arena =
        ArenaDefinition.draft(ArenaId.parse("arena"), NOW)
            .withTemplate("desert", "hbw_template_desert", ArenaStatus.INVALID, NOW);
    var codes =
        codes(new ArenaValidator(world -> false, id -> ArenaMapTemplateStatus.VALID), arena);
    assertTrue(codes.stream().noneMatch(code -> code.equals("unknown-world")));
  }

  private static String mapCode(ArenaDefinition arena, ArenaMapTemplateStatus status) {
    return codes(new ArenaValidator(world -> true, id -> status), arena).stream()
        .filter(code -> code.startsWith("MAP_TEMPLATE_"))
        .findFirst()
        .orElseThrow();
  }

  private static java.util.List<String> codes(ArenaValidator validator, ArenaDefinition arena) {
    return validator.validate(arena).problems().stream().map(ArenaProblem::code).toList();
  }
}
