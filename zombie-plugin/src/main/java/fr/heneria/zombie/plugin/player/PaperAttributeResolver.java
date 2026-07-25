package fr.heneria.zombie.plugin.player;

import java.lang.reflect.Field;
import org.bukkit.attribute.Attribute;

/**
 * Resolves Bukkit attributes whose constant names changed between Paper 1.21 maintenance releases.
 *
 * <p>The lookup happens once when this class is initialized. It deliberately avoids a bytecode
 * reference to either renamed field so the same plugin JAR can run on the complete Paper 1.21
 * series.
 */
final class PaperAttributeResolver {

  private static final Attribute MAX_HEALTH = resolveAttribute("MAX_HEALTH", "GENERIC_MAX_HEALTH");

  private PaperAttributeResolver() {}

  /**
   * Returns the maximum-health attribute exposed by the active Paper server.
   *
   * @return compatible maximum-health attribute
   */
  static Attribute maxHealth() {
    return MAX_HEALTH;
  }

  private static Attribute resolveAttribute(String... candidateNames) {
    for (String candidateName : candidateNames) {
      try {
        Field field = Attribute.class.getField(candidateName);
        Object value = field.get(null);
        if (value instanceof Attribute attribute) {
          return attribute;
        }
      } catch (NoSuchFieldException ignored) {
        // Try the name used by the next Paper 1.21 maintenance family.
      } catch (IllegalAccessException inaccessible) {
        throw new IllegalStateException(
            "Cannot access Bukkit attribute " + candidateName, inaccessible);
      }
    }
    throw new IllegalStateException(
        "This Paper build exposes no supported maximum-health attribute");
  }
}
