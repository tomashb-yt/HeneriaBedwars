package fr.heneria.bedwars.plugin.game;

import java.lang.reflect.Method;
import org.bukkit.scoreboard.Objective;

/** Optional Paper capability with a no-op fallback on the Spigot API target. */
final class ScoreboardNumberHider {
  private ScoreboardNumberHider() {}

  static boolean tryHide(Objective objective) {
    try {
      Class<?> formatType = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
      Object blank = formatType.getMethod("blank").invoke(null);
      Method setter;
      try {
        setter = objective.getClass().getMethod("numberFormat", formatType);
      } catch (NoSuchMethodException ignored) {
        setter = objective.getClass().getMethod("setNumberFormat", formatType);
      }
      setter.invoke(objective, blank);
      return true;
    } catch (ReflectiveOperationException | LinkageError unsupported) {
      return false;
    }
  }
}
