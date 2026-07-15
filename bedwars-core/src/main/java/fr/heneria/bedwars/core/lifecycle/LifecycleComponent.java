package fr.heneria.bedwars.core.lifecycle;

/** A component that is started once and stopped once by a {@link LifecycleManager}. */
public interface LifecycleComponent {
  /** Returns the stable name used in lifecycle logs. */
  String name();

  /** Starts the component and throws when it cannot become operational. */
  void start() throws Exception;

  /** Releases every resource owned by the component. */
  void stop() throws Exception;
}
