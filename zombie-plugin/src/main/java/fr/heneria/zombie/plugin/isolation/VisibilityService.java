package fr.heneria.zombie.plugin.isolation;

import fr.heneria.zombie.core.isolation.VisibilityPolicy;
import fr.heneria.zombie.core.session.PlayerSessionService;
import fr.heneria.zombie.core.session.PlayerSessionSnapshot;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Applies context visibility and therefore tablist isolation through Paper hide/show APIs. */
public final class VisibilityService {

  private final Plugin plugin;
  private final PlayerSessionService sessions;
  private final VisibilityPolicy policy;

  /**
   * Creates the visibility service.
   *
   * @param plugin owning plugin
   * @param sessions session source
   * @param policy pure policy
   */
  public VisibilityService(Plugin plugin, PlayerSessionService sessions, VisibilityPolicy policy) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  /** Recalculates every online viewer/subject pair after a context change. */
  public void refreshAll() {
    for (Player viewer : Bukkit.getOnlinePlayers()) {
      PlayerSessionSnapshot viewerSession = sessions.findSession(viewer.getUniqueId()).orElse(null);
      for (Player subject : Bukkit.getOnlinePlayers()) {
        if (viewer.equals(subject)) {
          continue;
        }
        PlayerSessionSnapshot subjectSession =
            sessions.findSession(subject.getUniqueId()).orElse(null);
        if (viewerSession != null
            && subjectSession != null
            && policy.canSee(viewerSession, subjectSession)) {
          viewer.showPlayer(plugin, subject);
        } else {
          viewer.hidePlayer(plugin, subject);
        }
      }
    }
  }

  /**
   * Restores plugin-owned visibility overrides during shutdown.
   *
   * @param player viewer
   */
  public void restore(Player player) {
    for (Player subject : Bukkit.getOnlinePlayers()) {
      if (!player.equals(subject)) {
        player.showPlayer(plugin, subject);
      }
    }
  }
}
