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
public final class PaperAttributeResolver {

  private static final Attribute MAX_HEALTH = resolveAttribute("MAX_HEALTH", "GENERIC_MAX_HEALTH");
  private static final Attribute ATTACK_DAMAGE =
      resolveAttribute("ATTACK_DAMAGE", "GENERIC_ATTACK_DAMAGE");
  private static final Attribute MOVEMENT_SPEED =
      resolveAttribute("MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
  private static final Attribute FOLLOW_RANGE =
      resolveAttribute("FOLLOW_RANGE", "GENERIC_FOLLOW_RANGE");
  private static final Attribute KNOCKBACK_RESISTANCE =
      resolveAttribute("KNOCKBACK_RESISTANCE", "GENERIC_KNOCKBACK_RESISTANCE");
  private static final Attribute ATTACK_KNOCKBACK =
      resolveAttribute("ATTACK_KNOCKBACK", "GENERIC_ATTACK_KNOCKBACK");

  private PaperAttributeResolver() {}

  /**
   * Returns the maximum-health attribute exposed by the active Paper server.
   *
   * @return compatible maximum-health attribute
   */
  public static Attribute maxHealth() {
    return MAX_HEALTH;
  }

  public static Attribute attackDamage() {
    return ATTACK_DAMAGE;
  }

  public static Attribute movementSpeed() {
    return MOVEMENT_SPEED;
  }

  public static Attribute followRange() {
    return FOLLOW_RANGE;
  }

  public static Attribute knockbackResistance() {
    return KNOCKBACK_RESISTANCE;
  }

  public static Attribute attackKnockback() {
    return ATTACK_KNOCKBACK;
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
        "This Paper build exposes no supported attribute from "
            + java.util.Arrays.toString(candidateNames));
  }
}
