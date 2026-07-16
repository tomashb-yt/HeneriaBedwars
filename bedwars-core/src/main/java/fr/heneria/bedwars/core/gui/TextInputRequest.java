package fr.heneria.bedwars.core.gui;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Immutable validation and callback contract for a single chat answer. */
public record TextInputRequest(
    String prompt,
    Duration timeout,
    int maximumLength,
    Set<String> cancelKeywords,
    Function<String, Optional<String>> validator,
    Consumer<String> onSuccess,
    Consumer<String> onInvalid,
    Consumer<TextInputCancelReason> onCancel) {
  public TextInputRequest {
    prompt = Objects.requireNonNull(prompt, "prompt");
    timeout = Objects.requireNonNull(timeout, "timeout");
    if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("timeout");
    if (maximumLength < 1 || maximumLength > 256)
      throw new IllegalArgumentException("maximumLength");
    cancelKeywords =
        Objects.requireNonNull(cancelKeywords, "cancelKeywords").stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    validator = Objects.requireNonNull(validator, "validator");
    onSuccess = Objects.requireNonNull(onSuccess, "onSuccess");
    onInvalid = Objects.requireNonNull(onInvalid, "onInvalid");
    onCancel = Objects.requireNonNull(onCancel, "onCancel");
  }
}
