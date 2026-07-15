package fr.heneria.bedwars.core.gui;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.TranslationKey;
import java.util.UUID;

/** Operations supplied by the platform adapter to click actions. */
public interface GuiRuntime {
  UUID playerId();

  String playerName();

  boolean hasPermission(String permission);

  void open(Gui gui);

  void replace(Gui gui);

  void back();

  void root();

  void refresh();

  void close();

  void playSound(String soundId);

  void message(TranslationKey key, PlaceholderContext placeholders);
}
