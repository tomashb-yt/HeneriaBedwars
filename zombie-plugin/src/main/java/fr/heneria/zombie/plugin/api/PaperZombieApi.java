package fr.heneria.zombie.plugin.api;

import fr.heneria.zombie.api.PluginState;
import fr.heneria.zombie.api.ZombieApi;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;

/** Thread-safe implementation of the public, read-only API. */
public final class PaperZombieApi implements ZombieApi {

  private final AtomicReference<PluginState> state;
  private final IntSupplier mapCount;
  private final IntSupplier instanceCount;

  /**
   * Creates the API facade.
   *
   * @param state shared lifecycle state
   * @param mapCount validated map count provider
   * @param instanceCount active instance count provider
   */
  public PaperZombieApi(
      AtomicReference<PluginState> state, IntSupplier mapCount, IntSupplier instanceCount) {
    this.state = Objects.requireNonNull(state, "state");
    this.mapCount = Objects.requireNonNull(mapCount, "mapCount");
    this.instanceCount = Objects.requireNonNull(instanceCount, "instanceCount");
  }

  @Override
  public PluginState state() {
    return state.get();
  }

  @Override
  public int registeredMapCount() {
    return Math.max(0, mapCount.getAsInt());
  }

  @Override
  public int activeInstanceCount() {
    return Math.max(0, instanceCount.getAsInt());
  }
}
