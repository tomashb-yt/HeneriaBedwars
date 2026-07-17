package fr.heneria.bedwars.core.game;

import java.util.Objects;
import java.util.Optional;

public record BedDestroyResult(BedDestroyCode code, Optional<RuntimeBed> bed) {
  public BedDestroyResult {
    code = Objects.requireNonNull(code, "code");
    bed = Objects.requireNonNull(bed, "bed");
  }

  public static BedDestroyResult of(BedDestroyCode code) {
    return new BedDestroyResult(code, Optional.empty());
  }

  public static BedDestroyResult destroyed(RuntimeBed bed) {
    return new BedDestroyResult(BedDestroyCode.DESTROYED, Optional.of(bed));
  }

  public boolean successful() {
    return code == BedDestroyCode.DESTROYED;
  }
}
