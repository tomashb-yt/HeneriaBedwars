package fr.heneria.zombie.plugin.gui;

import java.time.Instant;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/** Pending dangerous action displayed by the shared confirmation screen. */
public record GuiConfirmation(
    Component action,
    Component target,
    Component consequences,
    Instant confirmAfter,
    GuiAction confirmed) {

  /** Validates confirmation data. */
  public GuiConfirmation {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(consequences, "consequences");
    Objects.requireNonNull(confirmAfter, "confirmAfter");
    Objects.requireNonNull(confirmed, "confirmed");
  }

  /**
   * Returns whether the safety delay elapsed.
   *
   * @param now current instant
   * @return confirmation availability
   */
  public boolean availableAt(Instant now) {
    return !Objects.requireNonNull(now, "now").isBefore(confirmAfter);
  }
}
