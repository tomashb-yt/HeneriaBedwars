package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.gui.ConfirmationGui;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiClickType;
import fr.heneria.bedwars.core.gui.GuiSlots;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import java.util.List;
import java.util.Map;

/** Builds harmless Ticket 003 demonstrations entirely from Ticket 004 item keys. */
public final class DemoMenuFactory {
  private static final int ELEMENTS = 50;
  private final ConfigurationService configurations;
  private final GuiService guiService;
  private final String pluginVersion;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public DemoMenuFactory(
      ConfigurationService configurations, GuiService guiService, String pluginVersion) {
    this.configurations = configurations;
    this.guiService = guiService;
    this.pluginVersion = pluginVersion;
  }

  public Gui main() {
    return Gui.builder()
        .id("demo")
        .title(text(TranslationKey.GUI_DEMO_TITLE))
        .rows(6)
        .fillEmptySlots(configurations.snapshot().menus().fillEmptySlots())
        .data("plugin_version", pluginVersion)
        .data("language", configurations.snapshot().plugin().locale())
        .button(13, information())
        .button(20, clickTest())
        .button(22, openButton("demo.pagination", pagination()))
        .button(24, openButton("demo.confirmation", confirmation()))
        .button(30, openButton("demo.submenu", submenu()))
        .button(32, standard.refresh())
        .button(
            40,
            GuiButton.builder()
                .itemKey("demo.controlled-error")
                .visibleWhen(context -> configurations.snapshot().plugin().debug())
                .onLeftClick(
                    context -> {
                      throw new IllegalStateException(
                          "Intentional controlled GUI demonstration error");
                    })
                .build())
        .button(49, standard.close())
        .build();
  }

  private GuiButton openButton(String itemKey, Gui destination) {
    return GuiButton.builder()
        .itemKey(itemKey)
        .onLeftClick(context -> context.open(destination))
        .build();
  }

  private GuiButton information() {
    return GuiButton.builder()
        .itemKey("demo.information")
        .itemPlaceholders(
            context ->
                Map.of(
                    "plugin_version", pluginVersion,
                    "language", configurations.snapshot().plugin().locale(),
                    "gui_sessions", guiService.openCount(),
                    "sessions", guiService.openCount(),
                    "refresh_count", context.session().refreshCount()))
        .build();
  }

  private GuiButton clickTest() {
    return GuiButton.builder()
        .itemKey("demo.click-test")
        .on(GuiClickType.LEFT, context -> context.message(TranslationKey.GUI_DEMO_LEFT))
        .on(GuiClickType.RIGHT, context -> context.message(TranslationKey.GUI_DEMO_RIGHT))
        .on(GuiClickType.SHIFT_LEFT, context -> context.message(TranslationKey.GUI_DEMO_SHIFT_LEFT))
        .on(
            GuiClickType.SHIFT_RIGHT,
            context -> context.message(TranslationKey.GUI_DEMO_SHIFT_RIGHT))
        .on(GuiClickType.MIDDLE, context -> context.message(TranslationKey.GUI_DEMO_MIDDLE))
        .build();
  }

  private Gui pagination() {
    List<Integer> slots = GuiSlots.rectangle(1, 1, 3, 7);
    int maxPage = (ELEMENTS + slots.size() - 1) / slots.size();
    Gui.Builder builder =
        Gui.builder()
            .id("demo.pagination")
            .title(text(TranslationKey.GUI_DEMO_PAGINATION_TITLE))
            .rows(6)
            .fillEmptySlots(true)
            .data("max_page", maxPage);
    for (int position = 0; position < slots.size(); position++) {
      final int offset = position;
      builder.button(
          slots.get(position),
          GuiButton.builder()
              .visibleWhen(context -> context.page() * slots.size() + offset < ELEMENTS)
              .itemKey("demo.pagination-entry")
              .itemPlaceholders(
                  context -> Map.of("element", context.page() * slots.size() + offset + 1))
              .build());
    }
    return builder
        .button(
            45,
            standard.previous(
                context -> {
                  context.session().page(context.session().page() - 1);
                  context.refresh();
                }))
        .button(
            53,
            standard.next(
                context -> {
                  context.session().page(Math.min(maxPage - 1, context.session().page() + 1));
                  context.refresh();
                }))
        .button(49, standard.back())
        .button(
            50,
            GuiButton.builder()
                .itemKey("gui.page-indicator")
                .itemPlaceholders(context -> Map.of("max_page", maxPage))
                .build())
        .build();
  }

  private Gui confirmation() {
    return ConfirmationGui.builder()
        .id("demo.confirmation")
        .title(text(TranslationKey.GUI_DEMO_CONFIRM_TITLE))
        .informationKey("demo.confirmation")
        .confirmItemKey("gui.confirm")
        .cancelItemKey("gui.cancel")
        .onConfirm(
            context -> {
              context.message(TranslationKey.GUI_DEMO_CONFIRMED);
              context.back();
            })
        .onCancel(
            context -> {
              context.message(TranslationKey.GUI_DEMO_CANCELLED);
              context.back();
            })
        .build();
  }

  private Gui submenu() {
    return Gui.builder()
        .id("demo.submenu")
        .title(text(TranslationKey.GUI_DEMO_SUBMENU_TITLE))
        .rows(3)
        .fillEmptySlots(true)
        .button(13, standard.information("demo.submenu-information"))
        .button(22, standard.back())
        .build();
  }

  private String text(TranslationKey key) {
    return configurations.language().message(key);
  }
}
