package fr.heneria.bedwars.plugin.logging;

import fr.heneria.bedwars.core.logging.ProjectLogger;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Adapts the standard Bukkit logger to the platform-neutral logging boundary. */
public final class BukkitProjectLogger implements ProjectLogger {
  private final Logger logger;
  private volatile boolean debug;

  public BukkitProjectLogger(Logger logger, boolean debug) {
    this.logger = Objects.requireNonNull(logger, "logger");
    this.debug = debug;
  }

  /** Updates diagnostic logging after a configuration snapshot is activated. */
  @Override
  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  @Override
  public void info(String message) {
    logger.info(message);
  }

  @Override
  public void warning(String message) {
    logger.warning(message);
  }

  @Override
  public void error(String message, Throwable cause) {
    logger.log(Level.SEVERE, message, cause);
  }

  @Override
  public void debug(String message) {
    if (debug) {
      logger.info("[Debug] " + message);
    }
  }
}
