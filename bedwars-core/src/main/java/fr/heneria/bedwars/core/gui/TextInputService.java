package fr.heneria.bedwars.core.gui;

import java.util.UUID;
import java.util.function.Consumer;

/** Future boundary for player text input; no Anvil GUI implementation exists yet. */
public interface TextInputService {
  void request(UUID playerId, String prompt, Consumer<String> result);
}
