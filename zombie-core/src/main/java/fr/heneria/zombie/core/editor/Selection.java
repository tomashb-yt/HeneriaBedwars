package fr.heneria.zombie.core.editor;

import java.util.Optional;

/** Two-point editor selection used by future volumes and current placement previews. */
public record Selection(Optional<MapPoint> first, Optional<MapPoint> second) {
  public static final Selection EMPTY = new Selection(Optional.empty(), Optional.empty());

  public Selection {
    first = first == null ? Optional.empty() : first;
    second = second == null ? Optional.empty() : second;
  }
}
