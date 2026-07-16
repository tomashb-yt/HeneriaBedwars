package fr.heneria.bedwars.core.arena;

import fr.heneria.bedwars.core.config.ProblemSeverity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure structural validation with world existence delegated through a port. */
public final class ArenaValidator {
  private final ArenaWorldResolver worlds;

  public ArenaValidator(ArenaWorldResolver worlds) {
    this.worlds = Objects.requireNonNull(worlds, "worlds");
  }

  public ArenaValidationResult validate(ArenaDefinition arena) {
    List<ArenaProblem> problems = new ArrayList<>();
    if (arena.configVersion() != ArenaDefinition.CURRENT_CONFIG_VERSION)
      error(problems, "unsupported-version", "config-version", "Unsupported arena version");
    if (arena.displayName().isBlank())
      error(problems, "blank-name", "display-name", "Display name is blank");
    if (arena.worldName().isEmpty()) {
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
        .filter(boundary -> !boundary.ordered())
        .ifPresent(
            boundary ->
                error(
                    problems, "invalid-boundary", "boundary", "Boundary minimum exceeds maximum"));
    return new ArenaValidationResult(problems);
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
