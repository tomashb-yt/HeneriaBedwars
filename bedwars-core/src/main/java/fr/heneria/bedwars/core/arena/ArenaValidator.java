package fr.heneria.bedwars.core.arena;

import fr.heneria.bedwars.core.config.ProblemSeverity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Pure structural validation with world existence delegated through a port. */
public final class ArenaValidator {
  private final ArenaWorldResolver worlds;
  private final ArenaMapTemplateResolver maps;
  private final boolean requireMapTemplate;

  public ArenaValidator(ArenaWorldResolver worlds) {
    this(worlds, mapId -> ArenaMapTemplateStatus.VALID, false);
  }

  public ArenaValidator(ArenaWorldResolver worlds, ArenaMapTemplateResolver maps) {
    this(worlds, maps, true);
  }

  private ArenaValidator(
      ArenaWorldResolver worlds, ArenaMapTemplateResolver maps, boolean requireMapTemplate) {
    this.worlds = Objects.requireNonNull(worlds, "worlds");
    this.maps = Objects.requireNonNull(maps, "maps");
    this.requireMapTemplate = requireMapTemplate;
  }

  public ArenaValidationResult validate(ArenaDefinition arena) {
    List<ArenaProblem> problems = new ArrayList<>();
    if (arena.configVersion() != ArenaDefinition.CURRENT_CONFIG_VERSION)
      error(problems, "unsupported-version", "config-version", "Unsupported arena version");
    if (arena.displayName().isBlank())
      error(problems, "blank-name", "display-name", "Display name is blank");
    if (requireMapTemplate) {
      validateMapTemplate(problems, arena);
      if (arena.template().isPresent() && arena.worldName().isEmpty())
        error(problems, "missing-world", "world", "No world is configured");
    } else if (arena.worldName().isEmpty()) {
      error(problems, "missing-world", "world", "No world is configured");
    } else if (!worlds.exists(arena.worldName().orElseThrow())) {
      error(problems, "unknown-world", "world", "Configured world is not loaded");
    }
    if (arena.minimumPlayers() < 1)
      error(problems, "invalid-minimum", "players.minimum", "Minimum must be positive");
    if (arena.maximumPlayers() < arena.minimumPlayers())
      error(problems, "invalid-maximum", "players.maximum", "Maximum must be at least minimum");
    if (arena.teamCount() < 2)
      error(problems, "invalid-team-count", "teams.count", "At least two teams are required");
    if (arena.playersPerTeam() < 1)
      error(problems, "invalid-team-size", "teams.players-per-team", "Team size must be positive");
    if (arena.teamCount() > 0
        && arena.playersPerTeam() > 0
        && arena.maximumPlayers() != arena.teamCount() * arena.playersPerTeam())
      error(problems, "capacity-mismatch", "players.maximum", "Maximum must equal team capacity");
    validateTeams(problems, arena);
    if (arena.waitingLocation().isEmpty())
      error(problems, "missing-waiting", "locations.waiting", "Waiting location is missing");
    checkLocationWorld(problems, arena, arena.waitingLocation().orElse(null), "locations.waiting");
    if (arena.spectatorLocation().isEmpty()) {
      problems.add(
          new ArenaProblem(
              ProblemSeverity.WARNING,
              "missing-spectator",
              "locations.spectator",
              "Spectator location is missing"));
    } else {
      checkLocationWorld(
          problems, arena, arena.spectatorLocation().orElseThrow(), "locations.spectator");
    }
    arena
        .boundary()
        .ifPresent(
            boundary -> {
              if (boundary.enabled()
                  && (boundary.minimum().isEmpty() || boundary.maximum().isEmpty()))
                error(
                    problems,
                    "incomplete-boundary",
                    "boundary",
                    "Enabled boundary needs two points");
              else if (boundary.minimum().isPresent()
                  && boundary.maximum().isPresent()
                  && !boundary.ordered())
                error(problems, "invalid-boundary", "boundary", "Boundary minimum exceeds maximum");
            });
    return new ArenaValidationResult(problems);
  }

  private static void validateTeams(List<ArenaProblem> problems, ArenaDefinition arena) {
    if (arena.teams().size() < 2) {
      error(
          problems,
          "missing-teams",
          "teams.definitions",
          "At least two team definitions are required");
      return;
    }
    HashSet<TeamId> ids = new HashSet<>();
    HashSet<TeamColor> colors = new HashSet<>();
    HashSet<Integer> orders = new HashSet<>();
    int total = 0;
    for (ArenaTeamDefinition team : arena.teams()) {
      total += team.capacity();
      if (!ids.add(team.id()))
        error(problems, "duplicate-team-id", "teams.definitions", "Team id is duplicated");
      if (!colors.add(team.color()))
        error(problems, "duplicate-team-color", "teams.definitions", "Team color is duplicated");
      if (!orders.add(team.order()))
        error(problems, "duplicate-team-order", "teams.definitions", "Team order is duplicated");
      if (team.spawn().isEmpty())
        error(
            problems,
            "missing-team-spawn",
            "teams." + team.id().value() + ".spawn",
            "Team spawn is missing");
      else
        checkLocationWorld(
            problems, arena, team.spawn().orElseThrow(), "teams." + team.id().value() + ".spawn");
    }
    if (total != arena.maximumPlayers())
      error(
          problems,
          "team-capacity-mismatch",
          "teams.definitions",
          "Team capacities must equal maximum players");
  }

  private void validateMapTemplate(List<ArenaProblem> problems, ArenaDefinition arena) {
    if (arena.template().isEmpty()) {
      error(problems, "MAP_TEMPLATE_MISSING", "map.template-id", "No map template is associated");
      return;
    }
    switch (maps.status(arena.template().orElseThrow())) {
      case VALID -> {}
      case NOT_FOUND ->
          error(
              problems,
              "MAP_TEMPLATE_NOT_FOUND",
              "map.template-id",
              "Associated map template does not exist");
      case INVALID_TYPE ->
          error(
              problems,
              "MAP_TEMPLATE_INVALID_TYPE",
              "map.template-id",
              "Associated map is not a BedWars template");
      case ERROR ->
          error(
              problems,
              "MAP_TEMPLATE_ERROR",
              "map.template-id",
              "Associated map template is in error");
    }
  }

  private static void checkLocationWorld(
      List<ArenaProblem> problems, ArenaDefinition arena, ArenaLocation location, String field) {
    if (location != null
        && arena.worldName().isPresent()
        && !location.world().equals(arena.worldName().orElseThrow()))
      error(problems, "location-world-mismatch", field, "Location is in a different world");
  }

  private static void error(
      List<ArenaProblem> problems, String code, String field, String message) {
    problems.add(new ArenaProblem(ProblemSeverity.ERROR, code, field, message));
  }
}
