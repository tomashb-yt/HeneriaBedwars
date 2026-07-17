package fr.heneria.bedwars.core.game.display;

import fr.heneria.bedwars.core.config.MessageRenderer;
import fr.heneria.bedwars.core.config.PlaceholderContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Renders configured sidebar templates while preserving empty and visually duplicate lines. */
public final class RuntimeScoreboardRenderer {
  private final MessageRenderer renderer;

  public RuntimeScoreboardRenderer(MessageRenderer renderer) {
    this.renderer = Objects.requireNonNull(renderer, "renderer");
  }

  public RuntimeScoreboardView render(
      String title, List<String> templates, Map<String, ?> placeholders) {
    PlaceholderContext.Builder context = PlaceholderContext.builder();
    placeholders.forEach(context::put);
    PlaceholderContext values = context.build();
    return new RuntimeScoreboardView(
        renderer.render(title, values),
        templates.stream().map(line -> renderer.render(line, values)).toList());
  }
}
