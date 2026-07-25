package fr.heneria.zombie.core.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class GameInstanceServiceTest {

  @Test
  void createsAndCleansAnIsolatedWorld() {
    GameInstanceRegistry registry = new GameInstanceRegistry();
    AtomicBoolean destroyed = new AtomicBoolean();
    WorldInstanceGateway gateway =
        new WorldInstanceGateway() {
          @Override
          public CompletableFuture<WorldInstanceHandle> prepare(UUID id, String mapId) {
            return CompletableFuture.completedFuture(new WorldInstanceHandle("hz_" + id));
          }

          @Override
          public CompletableFuture<Boolean> destroy(
              WorldInstanceHandle handle, boolean preserveOnFailure) {
            destroyed.set(true);
            return CompletableFuture.completedFuture(true);
          }
        };
    GameInstanceService service = service(registry, gateway);

    GameInstanceSnapshot created =
        service.createInstance("crypt", GameInstanceOptions.publicGame(4)).join();
    assertEquals(GameInstanceState.WAITING, created.state());
    assertEquals(1, registry.size());
    service.closeInstance(created.id()).join();
    assertEquals(0, registry.size());
    assertEquals(true, destroyed.get());
  }

  @Test
  void removesRegistryEntryWhenPreparationFails() {
    GameInstanceRegistry registry = new GameInstanceRegistry();
    WorldInstanceGateway gateway =
        new WorldInstanceGateway() {
          @Override
          public CompletableFuture<WorldInstanceHandle> prepare(UUID id, String mapId) {
            return CompletableFuture.failedFuture(new IllegalStateException("copy failed"));
          }

          @Override
          public CompletableFuture<Boolean> destroy(
              WorldInstanceHandle handle, boolean preserveOnFailure) {
            return CompletableFuture.completedFuture(true);
          }
        };

    assertThrows(
        CompletionException.class,
        () ->
            service(registry, gateway)
                .createInstance("crypt", GameInstanceOptions.publicGame(4))
                .join());
    assertEquals(0, registry.size());
  }

  private static GameInstanceService service(
      GameInstanceRegistry registry, WorldInstanceGateway gateway) {
    return new GameInstanceService(
        registry,
        gateway,
        () -> 10,
        () -> true,
        () -> Duration.ofSeconds(1),
        UUID::randomUUID,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
        ignored -> {});
  }
}
