package fr.heneria.zombie.plugin.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class GuiSafetyPolicyTest {

  @Test
  void resolvesPermissionVisibilityExplicitly() {
    assertEquals(GuiPermissionPolicy.Visibility.ENABLED, GuiPermissionPolicy.evaluate(true, false));
    assertEquals(GuiPermissionPolicy.Visibility.LOCKED, GuiPermissionPolicy.evaluate(false, true));
    assertEquals(GuiPermissionPolicy.Visibility.HIDDEN, GuiPermissionPolicy.evaluate(false, false));
  }

  @Test
  void confirmationHonorsItsDelay() {
    GuiConfirmation confirmation =
        new GuiConfirmation(
            Component.text("delete"),
            Component.text("instance"),
            Component.text("cleanup"),
            Instant.ofEpochSecond(10),
            ignored -> {});
    assertFalse(confirmation.availableAt(Instant.ofEpochSecond(9)));
    assertTrue(confirmation.availableAt(Instant.ofEpochSecond(10)));
  }

  @Test
  void inputValidationAndExpiryAreIndependentPerRequest() {
    AtomicBoolean accepted = new AtomicBoolean();
    GuiInputRequest request =
        new GuiInputRequest(
            Component.text("input"),
            Instant.ofEpochSecond(20),
            value ->
                value.matches("[a-z]+")
                    ? GuiInputRequest.Validation.accept()
                    : GuiInputRequest.Validation.reject(Component.text("invalid")),
            ignored -> accepted.set(true),
            () -> {});

    assertTrue(request.validator().apply("map").accepted());
    assertFalse(request.validator().apply("42").accepted());
    assertTrue(Instant.ofEpochSecond(21).isAfter(request.expiresAt()));
  }
}
