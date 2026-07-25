package fr.heneria.zombie.plugin.player;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PaperAttributeResolverTest {

  @Test
  void resolvesMaximumHealthOnTheMinimumSupportedPaperApi() {
    assertNotNull(PaperAttributeResolver.maxHealth());
  }
}
