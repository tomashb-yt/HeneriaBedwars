package fr.heneria.zombie.core.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServiceRegistryTest {

  @Test
  void registersResolvesAndClearsOwnedServices() {
    ServiceRegistry registry = new ServiceRegistry();
    Runnable service = () -> {};

    registry.register(Runnable.class, service);

    assertSame(service, registry.require(Runnable.class));
    assertSame(service, registry.find(Runnable.class).orElseThrow());
    assertEquals(1, registry.size());

    registry.clear();
    assertTrue(registry.find(Runnable.class).isEmpty());
  }

  @Test
  void rejectsDuplicateAndMissingServices() {
    ServiceRegistry registry = new ServiceRegistry();
    registry.register(CharSequence.class, "first");

    assertThrows(
        IllegalStateException.class, () -> registry.register(CharSequence.class, "second"));
    assertThrows(IllegalStateException.class, () -> registry.require(Runnable.class));
  }
}
