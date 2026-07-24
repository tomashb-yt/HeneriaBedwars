package fr.heneria.zombie.core.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Small, explicitly owned registry used by the composition root.
 *
 * <p>Required services fail fast; optional services are represented by {@link Optional}. This
 * registry is not static and must never become a global service locator.
 */
public final class ServiceRegistry {

  private final Map<Class<?>, Object> services = new LinkedHashMap<>();

  /**
   * Registers exactly one implementation for a contract.
   *
   * @param contract exposed contract
   * @param service implementation
   * @param <T> contract type
   * @throws IllegalStateException when the contract is already registered
   */
  public synchronized <T> void register(Class<T> contract, T service) {
    Objects.requireNonNull(contract, "contract");
    Objects.requireNonNull(service, "service");
    if (services.containsKey(contract)) {
      throw new IllegalStateException("Service already registered: " + contract.getName());
    }
    services.put(contract, contract.cast(service));
  }

  /**
   * Resolves a mandatory service.
   *
   * @param contract requested contract
   * @param <T> contract type
   * @return registered implementation
   * @throws IllegalStateException when the service is absent
   */
  public synchronized <T> T require(Class<T> contract) {
    Objects.requireNonNull(contract, "contract");
    Object service = services.get(contract);
    if (service == null) {
      throw new IllegalStateException("Missing required service: " + contract.getName());
    }
    return contract.cast(service);
  }

  /**
   * Resolves an optional service.
   *
   * @param contract requested contract
   * @param <T> contract type
   * @return optional implementation
   */
  public synchronized <T> Optional<T> find(Class<T> contract) {
    Objects.requireNonNull(contract, "contract");
    return Optional.ofNullable(services.get(contract)).map(contract::cast);
  }

  /**
   * Returns the number of registered contracts.
   *
   * @return registry size
   */
  public synchronized int size() {
    return services.size();
  }

  /** Removes every reference owned by this registry. */
  public synchronized void clear() {
    services.clear();
  }
}
