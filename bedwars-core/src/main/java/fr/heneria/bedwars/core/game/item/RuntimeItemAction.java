package fr.heneria.bedwars.core.game.item;

import java.util.Arrays;
import java.util.Optional;

/** Stable runtime actions stored in the item PDC, never inferred from appearance. */
public enum RuntimeItemAction {
  WAITING_LEAVE("waiting_leave"),
  WAITING_INFO("waiting_info");

  private final String key;

  RuntimeItemAction(String key) {
    this.key = key;
  }

  public String key() {
    return key;
  }

  public static Optional<RuntimeItemAction> find(String key) {
    if (key == null) return Optional.empty();
    return Arrays.stream(values()).filter(action -> action.key.equals(key)).findFirst();
  }
}
