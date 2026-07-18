package fr.heneria.bedwars.core.statistics;

/** Idempotent outcome of one durable match write. */
public enum MatchRecordResult {
  RECORDED,
  ALREADY_RECORDED
}
