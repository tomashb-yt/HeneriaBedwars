package fr.heneria.bedwars.core.arena;

/** Resolves a persisted map id while keeping arena validation independent of Bukkit. */
@FunctionalInterface
public interface ArenaMapTemplateResolver {
  ArenaMapTemplateStatus status(String mapId);
}
