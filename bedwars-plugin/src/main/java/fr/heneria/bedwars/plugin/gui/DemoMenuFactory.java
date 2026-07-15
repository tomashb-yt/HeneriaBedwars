package fr.heneria.bedwars.plugin.gui;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.gui.ConfirmationGui;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiClickType;
import fr.heneria.bedwars.core.gui.GuiItem;
import fr.heneria.bedwars.core.gui.GuiSlots;
import java.util.List;

/** Builds the deliberately harmless menus used to exercise every Ticket 003 primitive. */
public final class DemoMenuFactory {
  private static final int ELEMENTS = 50;
  private final fr.heneria.bedwars.plugin.config.ConfigurationService configurations;
  private final GuiService guiService;
  private final String pluginVersion;
  private final StandardGuiButtons standard;

  public DemoMenuFactory(
      fr.heneria.bedwars.plugin.config.ConfigurationService configurations,
      GuiService guiService,
      String pluginVersion) {
    this.configurations = configurations;
    this.guiService = guiService;
    this.pluginVersion = pluginVersion;
    standard = new StandardGuiButtons(configurations.language());
  }

  public Gui main() {
    return Gui.builder()
        .id("demo")
        .title(text(TranslationKey.GUI_DEMO_TITLE))
        .rows(6)
        .fillEmptySlots(configurations.snapshot().menus().fillEmptySlots())
        .button(13, information())
        .button(20, clickTest())
        .button(
            22,
            GuiButton.builder()
                .item(GuiItem.of("BOOK", text(TranslationKey.GUI_DEMO_PAGINATION)))
                .onLeftClick(context -> context.open(pagination()))
                .build())
        .button(
            24,
            GuiButton.builder()
                .item(GuiItem.of("GOLD_INGOT", text(TranslationKey.GUI_DEMO_CONFIRMATION)))
                .onLeftClick(context -> context.open(confirmation()))
                .build())
        .button(
            30,
            GuiButton.builder()
                .item(GuiItem.of("ENDER_PEARL", text(TranslationKey.GUI_DEMO_SUBMENU)))
                .onLeftClick(context -> context.open(submenu()))
                .build())
        .button(32, standard.refresh())
        .button(
            40,
            GuiButton.builder()
                .item(
                    GuiItem.of(
                        "TNT",
                        text(TranslationKey.GUI_DEMO_ERROR),
                        text(TranslationKey.GUI_DEMO_ERROR_LORE)))
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

  private GuiButton information() {
    return GuiButton.builder()
        .item(
            context -> {
              PlaceholderContext values =
                  PlaceholderContext.builder()
                      .put("player", context.playerName())
                      .put("plugin_version", pluginVersion)
                      .put("language", configurations.snapshot().plugin().locale())
                      .put("sessions", guiService.openCount())
                      .put("refresh_count", context.session().refreshCount())
                      .build();
              return new GuiItem(
                  "PLAYER_HEAD",
                  1,
                  text(TranslationKey.GUI_DEMO_INFORMATION),
                  List.of(
                      message(TranslationKey.GUI_DEMO_PLAYER, values),
                      message(TranslationKey.GUI_DEMO_VERSION, values),
                      message(TranslationKey.GUI_DEMO_LANGUAGE, values),
                      message(TranslationKey.GUI_DEMO_SESSIONS, values),
                      message(TranslationKey.GUI_DEMO_REFRESH_COUNT, values)),
                  false,
                  null);
            })
        .build();
  }

  private GuiButton clickTest() {
    return GuiButton.builder()
        .item(GuiItem.of("LEVER", text(TranslationKey.GUI_DEMO_CLICK)))
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
    Gui.Builder builder =
        Gui.builder()
            .id("demo.pagination")
            .title(text(TranslationKey.GUI_DEMO_PAGINATION_TITLE))
            .rows(6)
            .fillEmptySlots(true);
    for (int position = 0; position < slots.size(); position++) {
      final int offset = position;
      builder.button(
          slots.get(position),
          GuiButton.builder()
              .visibleWhen(context -> context.page() * slots.size() + offset < ELEMENTS)
              .item(
                  context ->
                      GuiItem.of(
                          "PAPER",
                          message(
                              TranslationKey.GUI_DEMO_ELEMENT,
                              PlaceholderContext.builder()
                                  .put("element", context.page() * slots.size() + offset + 1)
                                  .build())))
              .build());
    }
    int maxPage = (ELEMENTS + slots.size() - 1) / slots.size();
    builder
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
                .item(
                    context ->
                        GuiItem.of(
                            "MAP",
                            message(
                                TranslationKey.GUI_DEMO_PAGE,
                                PlaceholderContext.builder()
                                    .put("page", context.page() + 1)
                                    .put("max_page", maxPage)
                                    .build())))
                .build());
    return builder.build();
  }

  private Gui confirmation() {
    return ConfirmationGui.builder()
        .id("demo.confirmation")
        .title(text(TranslationKey.GUI_DEMO_CONFIRM_TITLE))
        .information(GuiItem.of("PAPER", text(TranslationKey.GUI_DEMO_CONFIRMATION)))
        .confirmItem(standard.confirmItem())
        .cancelItem(standard.cancelItem())
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
        .button(
            13,
            standard.information(GuiItem.of("PAPER", text(TranslationKey.GUI_DEMO_SUBMENU_INFO))))
        .button(22, standard.back())
        .build();
  }

  private String text(TranslationKey key) {
    return configurations.language().message(key);
  }

  private String message(TranslationKey key, PlaceholderContext context) {
    return configurations.language().message(key, context);
  }
}
