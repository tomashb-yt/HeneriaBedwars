package fr.heneria.bedwars.core.item;

/** Wraps a safe item-construction failure while preserving its logical key. */
public final class ItemBuildException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** Wraps the original construction error without losing the logical item key. */
  public ItemBuildException(String key, Throwable cause) {
    super("Unable to build item '" + key + "'", cause);
  }
}
