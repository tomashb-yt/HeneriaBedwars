package fr.heneria.zombie.core.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GameInstanceRegistryTest {

  @Test
  void rejectsDuplicateIdentifiersAndFindsMembership() {
    UUID id = UUID.randomUUID();
    UUID player = UUID.randomUUID();
    GameInstance first =
        new GameInstance(id, "crypt", GameInstanceOptions.publicGame(4), Instant.EPOCH);
    first.markPrepared(new WorldInstanceHandle("hz_first"));
    first.addPlayer(player);
    GameInstance duplicate =
        new GameInstance(id, "crypt", GameInstanceOptions.publicGame(4), Instant.EPOCH);
    GameInstanceRegistry registry = new GameInstanceRegistry();

    registry.register(first);
    assertThrows(IllegalStateException.class, () -> registry.register(duplicate));
    assertEquals(1, registry.containing(player).size());
    assertEquals(1, registry.snapshots().size());
    registry.remove(first);
    assertEquals(0, registry.size());
  }
}
