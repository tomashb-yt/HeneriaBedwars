package fr.heneria.bedwars.core.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Small type-safe registry for internal services.
 *
 * <p>One implementation may be registered per exact type. Required lookup fails explicitly while
 * optional lookup uses {@link Optional}; this class never returns {@code null}.
 */
public final class ServiceRegistry {
  private final Map<Class<?>, Object> services = new LinkedHashMap<>();

  /** Registers exactly one non-null implementation for an exact service type. */
  public <T> void register(Class<T> type, T implementation) {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(implementation, "implementation");
    if (!type.isInstance(implementation)) {
      throw new IllegalArgumentException(
          implementation.getClass().getName() + " does not implement " + type.getName());
    }
    if (services.containsKey(type)) {
      throw new DuplicateServiceException(type);
    }
    services.put(type, implementation);
  }

  /** Returns a mandatory service or throws an explicit {@link MissingServiceException}. */
  public <T> T require(Class<T> type) {
    return find(type).orElseThrow(() -> new MissingServiceException(type));
  }

  /** Returns an optional service without ever returning {@code null}. */
  public <T> Optional<T> find(Class<T> type) {
    Objects.requireNonNull(type, "type");
    return Optional.ofNullable(services.get(type)).map(type::cast);
  }

  /** Returns the number of registered service types. */
  public int size() {
    return services.size();
  }
}
