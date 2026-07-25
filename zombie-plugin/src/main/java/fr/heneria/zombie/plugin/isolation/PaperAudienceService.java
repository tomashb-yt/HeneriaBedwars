package fr.heneria.zombie.plugin.isolation;

import fr.heneria.zombie.core.isolation.AudienceSelector;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Creates targeted Adventure audiences and never falls back to a global broadcast. */
public final class PaperAudienceService {

  private final AudienceSelector selector;

  /**
   * Creates the audience service.
   *
   * @param selector recipient selector
   */
  public PaperAudienceService(AudienceSelector selector) {
    this.selector = Objects.requireNonNull(selector, "selector");
  }

  /**
   * Returns the current lobby audience.
   *
   * @return targeted audience
   */
  public Audience lobby() {
    return audience(selector.lobby());
  }

  /**
   * Returns one instance audience.
   *
   * @param instanceId instance identifier
   * @return targeted audience
   */
  public Audience instance(UUID instanceId) {
    return audience(selector.instance(instanceId));
  }

  private static Audience audience(java.util.Set<UUID> players) {
    return Audience.audience(
        players.stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .map(Player.class::cast)
            .toList());
  }
}
