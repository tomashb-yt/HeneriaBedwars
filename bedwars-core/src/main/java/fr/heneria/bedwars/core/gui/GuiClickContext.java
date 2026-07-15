package fr.heneria.bedwars.core.gui;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.TranslationKey;
import java.util.Map;
import java.util.Objects;

/** Immutable context delivered to a GUI action on the server thread. */
public record GuiClickContext(
    GuiRuntime runtime,
    Gui gui,
    GuiSession session,
    int slot,
    GuiClickType clickType,
    Map<String, Object> data) {
  public GuiClickContext {
    Objects.requireNonNull(runtime, "runtime");
    Objects.requireNonNull(gui, "gui");
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(clickType, "clickType");
    data = Map.copyOf(data);
  }

  public void open(Gui target) {
    runtime.open(target);
  }

  public void replace(Gui target) {
    runtime.replace(target);
  }

  public void back() {
    runtime.back();
  }

  public void root() {
    runtime.root();
  }

  public void refresh() {
    runtime.refresh();
  }

  public void close() {
    runtime.close();
  }

  public void playSound(String id) {
    runtime.playSound(id);
  }

  public void message(TranslationKey key) {
    runtime.message(key, PlaceholderContext.EMPTY);
  }

  public void message(TranslationKey key, PlaceholderContext placeholders) {
    runtime.message(key, placeholders);
  }
}
