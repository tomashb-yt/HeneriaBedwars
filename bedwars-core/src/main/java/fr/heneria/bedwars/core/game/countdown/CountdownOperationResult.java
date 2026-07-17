package fr.heneria.bedwars.core.game.countdown;

import java.util.Optional;

/** Explicit result for countdown start, cancellation and force-start operations. */
public record CountdownOperationResult(
    CountdownOperationCode code, Optional<CountdownSnapshot> countdown, String detail) {
  public CountdownOperationResult {
    countdown = countdown == null ? Optional.empty() : countdown;
    detail = detail == null ? "" : detail.trim();
  }

  public boolean successful() {
    return code == CountdownOperationCode.SUCCESS;
  }

  public static CountdownOperationResult success(CountdownSnapshot countdown) {
    return new CountdownOperationResult(
        CountdownOperationCode.SUCCESS, Optional.ofNullable(countdown), "");
  }

  public static CountdownOperationResult failure(CountdownOperationCode code, String detail) {
    return new CountdownOperationResult(code, Optional.empty(), detail);
  }
}
