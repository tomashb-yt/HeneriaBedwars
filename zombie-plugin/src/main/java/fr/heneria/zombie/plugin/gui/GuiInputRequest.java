package fr.heneria.zombie.plugin.gui;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;

/** Interchangeable request for validated player text input. */
public record GuiInputRequest(
    Component prompt,
    Instant expiresAt,
    Function<String, Validation> validator,
    Consumer<String> accepted,
    Runnable cancelled) {

  /** Validates request collaborators. */
  public GuiInputRequest {
    Objects.requireNonNull(prompt, "prompt");
    Objects.requireNonNull(expiresAt, "expiresAt");
    Objects.requireNonNull(validator, "validator");
    Objects.requireNonNull(accepted, "accepted");
    Objects.requireNonNull(cancelled, "cancelled");
  }

  /**
   * Validates raw input.
   *
   * @param input raw text
   * @return validation result
   */
  public Validation validate(String input) {
    return Objects.requireNonNull(validator.apply(input), "validator result");
  }

  /** Input validation result. */
  public record Validation(boolean accepted, Component message) {

    /** Creates an accepted result. */
    public static Validation accept() {
      return new Validation(true, Component.empty());
    }

    /**
     * Creates a rejected result.
     *
     * @param message feedback
     * @return rejected result
     */
    public static Validation reject(Component message) {
      return new Validation(false, Objects.requireNonNull(message, "message"));
    }
  }
}
