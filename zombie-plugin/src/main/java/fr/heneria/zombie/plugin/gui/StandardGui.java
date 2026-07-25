package fr.heneria.zombie.plugin.gui;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Small reusable GUI implementation backed by a cached YAML template and render callback. */
public final class StandardGui implements Gui {

  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final GuiId id;
  private final GuiConfigurationService configurations;
  private final BiConsumer<GuiView, GuiContext> renderer;
  private final IntSupplier refreshTicks;

  public StandardGui(
      String id,
      GuiConfigurationService configurations,
      BiConsumer<GuiView, GuiContext> renderer,
      IntSupplier refreshTicks) {
    this.id = new GuiId(id);
    this.configurations = Objects.requireNonNull(configurations, "configurations");
    this.renderer = Objects.requireNonNull(renderer, "renderer");
    this.refreshTicks = Objects.requireNonNull(refreshTicks, "refreshTicks");
  }

  @Override
  public GuiId id() {
    return id;
  }

  @Override
  public Component title(GuiContext context) {
    return MINI.deserialize(template().title());
  }

  @Override
  public int size(GuiContext context) {
    return template().size();
  }

  @Override
  public void render(GuiView view, GuiContext context) {
    renderer.accept(view, context);
  }

  @Override
  public int refreshTicks(GuiContext context) {
    return Math.max(0, refreshTicks.getAsInt());
  }

  private GuiMenuTemplate template() {
    return configurations
        .current()
        .menu(id)
        .orElseThrow(() -> new IllegalStateException("Missing GUI template " + id.value()));
  }
}
