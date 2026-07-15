package fr.heneria.bedwars.core.lifecycle;

/** Internal state of a lifecycle manager. */
public enum LifecycleState {
  /** No component has started. */
  NEW,
  /** Components are starting. */
  STARTING,
  /** Every component started successfully. */
  RUNNING,
  /** Components are stopping in reverse order. */
  STOPPING,
  /** Every component stopped successfully. */
  STOPPED,
  /** Startup, rollback, or shutdown failed. */
  FAILED
}
