package fr.heneria.zombie.core.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Deterministic price pipeline: base + fixed, multipliers, rounding, then bounds. */
public final class PriceResolver {
  private final Rounding rounding;
  private final long minimum;
  private final long maximum;

  public PriceResolver(Rounding rounding, long minimum, long maximum) {
    if (minimum < 0 || maximum < minimum) {
      throw new IllegalArgumentException("Invalid price bounds");
    }
    this.rounding = java.util.Objects.requireNonNull(rounding, "rounding");
    this.minimum = minimum;
    this.maximum = maximum;
  }

  public PriceResult resolve(PriceContext context) {
    java.util.Objects.requireNonNull(context, "context");
    if (context.basePrice() < 0) {
      return new PriceResult(false, 0, "Negative base price");
    }
    BigDecimal value = BigDecimal.valueOf(context.basePrice());
    for (long fixed : context.fixedModifiers()) {
      value = value.add(BigDecimal.valueOf(fixed));
    }
    for (BigDecimal multiplier : context.multipliers()) {
      if (multiplier == null || multiplier.signum() < 0) {
        return new PriceResult(false, 0, "Invalid multiplier");
      }
      value = value.multiply(multiplier);
    }
    RoundingMode mode =
        switch (rounding) {
          case FLOOR -> RoundingMode.FLOOR;
          case CEIL -> RoundingMode.CEILING;
          case NEAREST -> RoundingMode.HALF_UP;
        };
    try {
      long resolved = value.setScale(0, mode).longValueExact();
      return new PriceResult(true, Math.max(minimum, Math.min(maximum, resolved)), "");
    } catch (ArithmeticException overflow) {
      return new PriceResult(false, 0, "Price overflow");
    }
  }

  public enum Rounding {
    FLOOR,
    CEIL,
    NEAREST
  }

  public record PriceContext(
      long basePrice, List<Long> fixedModifiers, List<BigDecimal> multipliers) {
    public PriceContext {
      fixedModifiers = fixedModifiers == null ? List.of() : List.copyOf(fixedModifiers);
      multipliers = multipliers == null ? List.of() : List.copyOf(multipliers);
    }

    public static PriceContext base(long amount) {
      return new PriceContext(amount, List.of(), List.of());
    }
  }

  public record PriceResult(boolean valid, long price, String failureReason) {}
}
