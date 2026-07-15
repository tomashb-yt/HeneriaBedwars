package fr.heneria.bedwars.core.config;

import java.util.Objects;
import java.util.function.Supplier;

/** Renders typed translations from the currently active snapshot. */
public final class LanguageService {
  private final Supplier<ConfigurationSnapshot> snapshotSupplier;
  private final MessageRenderer renderer;

  public LanguageService(
      Supplier<ConfigurationSnapshot> snapshotSupplier, MessageRenderer renderer) {
    this.snapshotSupplier = Objects.requireNonNull(snapshotSupplier, "snapshotSupplier");
    this.renderer = Objects.requireNonNull(renderer, "renderer");
  }

  public String message(TranslationKey key) {
    return message(key, PlaceholderContext.EMPTY);
  }

  public String message(TranslationKey key, PlaceholderContext context) {
    return renderer.render(snapshotSupplier.get().activeLanguage().message(key), context);
  }
}
