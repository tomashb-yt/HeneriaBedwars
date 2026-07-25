package fr.heneria.zombie.core.isolation;

import fr.heneria.zombie.core.session.PlayerContext;
import fr.heneria.zombie.core.session.PlayerSessionSnapshot;
import java.util.Objects;

/** Pure visibility policy shared by player visibility, tablist and chat. */
public final class VisibilityPolicy {

  /**
   * Returns whether two players belong to the same visible context.
   *
   * @param viewer viewer session
   * @param subject subject session
   * @return visibility decision
   */
  public boolean canSee(PlayerSessionSnapshot viewer, PlayerSessionSnapshot subject) {
    Objects.requireNonNull(viewer, "viewer");
    Objects.requireNonNull(subject, "subject");
    if (!viewer.online() || !subject.online()) {
      return false;
    }
    if (viewer.context() != subject.context()) {
      return false;
    }
    if (viewer.context() == PlayerContext.LOBBY) {
      return true;
    }
    return viewer.instanceId().equals(subject.instanceId());
  }
}
