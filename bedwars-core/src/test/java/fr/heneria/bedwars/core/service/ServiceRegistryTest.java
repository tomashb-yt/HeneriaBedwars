package fr.heneria.bedwars.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServiceRegistryTest {
  private final ServiceRegistry registry = new ServiceRegistry();

  @Test
  void registersAService() {
    registry.register(Runnable.class, () -> {});
    assertEquals(1, registry.size());
  }

  @Test
  void requiresARegisteredService() {
    Runnable service = () -> {};
    registry.register(Runnable.class, service);
    assertSame(service, registry.require(Runnable.class));
  }

  @Test
  void rejectsMissingRequiredService() {
    assertThrows(MissingServiceException.class, () -> registry.require(Runnable.class));
  }

  @Test
  void rejectsDuplicateService() {
    registry.register(Runnable.class, () -> {});
    assertThrows(
        DuplicateServiceException.class, () -> registry.register(Runnable.class, () -> {}));
  }

  @Test
  void findsAnOptionalService() {
    assertTrue(registry.find(Runnable.class).isEmpty());
  }
}
