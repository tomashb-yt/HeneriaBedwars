package fr.heneria.zombie.core.economy;

/** Validated limits applied to every wallet in one game. */
public record EconomyPolicy(
    long maximumBalance,
    OverflowPolicy overflowPolicy,
    boolean allowNegativeBalance,
    int maximumHistoryEntries) {
  public EconomyPolicy {
    if (maximumBalance < 0 || maximumHistoryEntries < 1) {
      throw new IllegalArgumentException("Invalid economy limits");
    }
    java.util.Objects.requireNonNull(overflowPolicy, "overflowPolicy");
  }

  public static EconomyPolicy defaults() {
    return new EconomyPolicy(999_999_999L, OverflowPolicy.LOG_AND_CLAMP, false, 1_000);
  }
}
