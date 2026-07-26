package fr.heneria.zombie.plugin.weapon;

import fr.heneria.zombie.core.weapon.WeaponDefinition;
import fr.heneria.zombie.plugin.game.PaperGameRuntime;
import fr.heneria.zombie.plugin.gui.GuiActionRegistry;
import fr.heneria.zombie.plugin.gui.GuiConfigurationService;
import fr.heneria.zombie.plugin.gui.GuiContext;
import fr.heneria.zombie.plugin.gui.GuiId;
import fr.heneria.zombie.plugin.gui.GuiPagination;
import fr.heneria.zombie.plugin.gui.GuiRegistry;
import fr.heneria.zombie.plugin.gui.GuiService;
import fr.heneria.zombie.plugin.gui.GuiView;
import fr.heneria.zombie.plugin.gui.StandardGui;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;

/** Configurable browser and statistics preview for every data-driven weapon definition. */
public final class WeaponGuiModule {
  private final GuiRegistry registry;
  private final GuiActionRegistry actions;
  private final GuiConfigurationService configurations;
  private final GuiService guis;
  private final PaperWeaponService weapons;
  private final PaperGameRuntime games;

  public WeaponGuiModule(
      GuiRegistry registry,
      GuiActionRegistry actions,
      GuiConfigurationService configurations,
      GuiService guis,
      PaperWeaponService weapons,
      PaperGameRuntime games) {
    this.registry = registry;
    this.actions = actions;
    this.configurations = configurations;
    this.guis = guis;
    this.weapons = weapons;
    this.games = games;
  }

  public void register() {
    actions.register(
        "nav.weapons",
        context -> guis.open(context.player(), new GuiId("weapon-browser"), GuiContext.EMPTY));
    registry.register(
        new StandardGui("weapon-browser", configurations, this::renderBrowser, () -> 0));
    registry.register(
        new StandardGui("weapon-detail", configurations, this::renderDetail, () -> 0));
  }

  private void renderBrowser(GuiView view, GuiContext ignored) {
    List<WeaponDefinition> values =
        weapons.types().stream()
            .sorted(java.util.Comparator.comparing(WeaponDefinition::id))
            .toList();
    var session = guis.session(view.player());
    GuiPagination.Page<WeaponDefinition> page =
        GuiPagination.page(values, session.page(), view.menu().contentSlots().size());
    session.page(page.index());
    for (int index = 0; index < page.items().size(); index++) {
      WeaponDefinition weapon = page.items().get(index);
      view.button(
          view.menu().contentSlots().get(index),
          material(weapon),
          "<gold>" + weapon.displayName(),
          List.of(
              "<gray>ID : <white>" + weapon.id(),
              "<gray>Catégorie : <white>" + weapon.category(),
              "<gray>Rareté : <white>" + weapon.rarity(),
              "<gray>Dégâts : <white>" + weapon.damage().baseDamage(),
              "<gray>Chargeur : <white>" + weapon.ammo().magazineSize(),
              "",
              "<yellow>Clic : statistiques",
              "<gold>Maj + clic : donner en partie"),
          "zombies.admin.weapon.types",
          click ->
              guis.open(
                  click.player(), new GuiId("weapon-detail"), GuiContext.of("weapon", weapon.id())),
          null,
          click ->
              games
                  .gameFor(click.player().getUniqueId())
                  .flatMap(
                      game -> weapons.give(game, click.player(), weapon, Bukkit.getCurrentTick()))
                  .ifPresentOrElse(
                      value -> click.player().sendMessage("Arme de test ajoutée."),
                      () -> click.player().sendMessage("Rejoignez d'abord une partie active.")));
    }
    view.configured("back");
    view.configured("previous");
    view.configured("next");
    view.configured("home");
  }

  private void renderDetail(GuiView view, GuiContext context) {
    WeaponDefinition weapon =
        context
            .value("weapon", String.class)
            .flatMap(
                id -> weapons.types().stream().filter(value -> value.id().equals(id)).findFirst())
            .orElse(null);
    if (weapon == null) {
      view.information(22, Material.BARRIER, "<red>Arme inconnue", List.of());
    } else {
      view.information(
          22,
          material(weapon),
          "<gold>" + weapon.displayName(),
          List.of(
              "<gray>Mode : <white>" + weapon.fire().mode(),
              "<gray>Cadence : <white>" + weapon.fire().cooldownTicks() + " ticks",
              "<gray>Projectiles : <white>" + weapon.fire().pellets(),
              "<gray>Portée : <white>" + weapon.damage().maximumDistance(),
              "<gray>Headshot : <white>x" + weapon.damage().headshotMultiplier(),
              "<gray>Pénétration : <white>" + weapon.penetration().maximumTargets(),
              "<gray>Rechargement : <white>" + weapon.reload().durationTicks() + " ticks",
              "<gray>Mur : <white>" + weapon.economy().wallCost() + " points",
              "<gray>Améliorations PAP : <white>" + weapon.upgrades().size(),
              "<gray>Effets : <white>" + weapon.effects()));
    }
    view.configured("back");
    view.configured("home");
  }

  private static Material material(WeaponDefinition weapon) {
    Material material = Material.matchMaterial(weapon.presentation().material());
    return material == null ? Material.CROSSBOW : material;
  }
}
