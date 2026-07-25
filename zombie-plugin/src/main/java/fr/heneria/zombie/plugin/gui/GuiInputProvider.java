package fr.heneria.zombie.plugin.gui;

import org.bukkit.entity.Player;

/** Interchangeable entry point for chat, anvil, book or sign input adapters. */
@FunctionalInterface
public interface GuiInputProvider {

  /**
   * Starts a private validated input request.
   *
   * @param player requesting player
   * @param request request lifecycle
   */
  void requestInput(Player player, GuiInputRequest request);
}
