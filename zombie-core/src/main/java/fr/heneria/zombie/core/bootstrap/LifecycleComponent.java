package fr.heneria.zombie.core.bootstrap;

/** A named resource participating in deterministic plugin startup and shutdown. */
public interface LifecycleComponent {

  /**
   * Returns the stable diagnostic name of this component.
   *
   * @return non-empty name
   */
  String name();

  /**
   * Starts the component.
   *
   * @throws Exception when the component cannot start completely
   */
  void start() throws Exception;

  /**
   * Stops the component and releases all owned resources.
   *
   * @throws Exception when one or more resources cannot be released
   */
  void stop() throws Exception;
}
