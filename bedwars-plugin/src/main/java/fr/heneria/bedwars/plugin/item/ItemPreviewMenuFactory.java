package fr.heneria.bedwars.plugin.item;

import fr.heneria.bedwars.core.command.AdministrativeCommandPolicy;
import fr.heneria.bedwars.core.config.TranslationKey;
import fr.heneria.bedwars.core.gui.Gui;
import fr.heneria.bedwars.core.gui.GuiButton;
import fr.heneria.bedwars.core.gui.GuiSlots;
import fr.heneria.bedwars.plugin.config.ConfigurationService;
import fr.heneria.bedwars.plugin.gui.StandardGuiButtons;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Paginated read-only preview of the active item registry. */
public final class ItemPreviewMenuFactory {
  private final ConfigurationService configurations;
  private final ItemService items;
  private final String pluginVersion;
  private final StandardGuiButtons standard = new StandardGuiButtons();

  public ItemPreviewMenuFactory(
      ConfigurationService configurations, ItemService items, String pluginVersion) {
    this.configurations = configurations;
    this.items = items;
    this.pluginVersion = pluginVersion;
  }

  public Gui create() {
    List<String> keys = items.registeredKeys().stream().sorted().toList();
    List<Integer> slots = GuiSlots.rectangle(1, 1, 3, 7);
    int maximumPage = Math.max(1, (keys.size() + slots.size() - 1) / slots.size());
    Gui.Builder menu =
        Gui.builder()
            .id("items.preview")
            .title(configurations.language().message(TranslationKey.ITEM_PREVIEW_TITLE))
            .rows(6)
            .fillEmptySlots(true)
            .data("max_page", maximumPage);
    for (int index = 0; index < slots.size(); index++) {
      final int offset = index;
      menu.button(
          slots.get(index),
          GuiButton.builder()
              .itemKey(
                  context -> {
                    int position = context.session().page() * slots.size() + offset;
                    return position < keys.size() ? keys.get(position) : "gui.error";
                  })
              .itemPlaceholders(
                  context -> {
                    int position = context.session().page() * slots.size() + offset;
                    return position < keys.size()
                        ? Map.of(
                            "preview_key",
                            keys.get(position),
                            "element",
                            position + 1,
                            "plugin_version",
                            pluginVersion,
                            "language",
                            configurations.snapshot().plugin().locale(),
                            "sessions",
                            0,
                            "refresh_count",
                            context.session().refreshCount())
                        : Map.of();
                  })
              .visibleWhen(context -> context.page() * slots.size() + offset < keys.size())
              .onLeftClick(
                  context -> {
                    int position = context.session().page() * slots.size() + offset;
                    if (position >= keys.size()) return;
                    Player player = Bukkit.getPlayer(context.runtime().playerId());
                    if (player == null) return;
                    if (player.getInventory().firstEmpty() < 0) {
                      context.message(TranslationKey.ITEM_INVENTORY_FULL);
                      return;
                    }
                    String key = keys.get(position);
                    ItemStack copy =
                        items.build(
                            key,
                            ItemContexts.forPlayer(player, configurations)
                                .placeholder("item_key", key)
                                .placeholder("plugin_version", pluginVersion)
                                .build());
                    player.getInventory().addItem(copy);
                    context.message(
                        TranslationKey.ITEM_GIVEN,
                        fr.heneria.bedwars.core.config.PlaceholderContext.builder()
                            .put("item_key", key)
                            .build());
                  })
              .permission(AdministrativeCommandPolicy.ITEM_GIVE)
              .build());
    }
    return menu.button(
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
                  context.session().page(Math.min(maximumPage - 1, context.session().page() + 1));
                  context.refresh();
                }))
        .button(48, standard.back())
        .button(50, standard.close())
        .build();
  }
}
