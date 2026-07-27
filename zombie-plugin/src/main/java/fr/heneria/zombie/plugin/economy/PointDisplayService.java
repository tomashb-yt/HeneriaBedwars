package fr.heneria.zombie.plugin.economy;

import fr.heneria.zombie.core.economy.EconomyEvent;
import fr.heneria.zombie.core.economy.Transaction;
import fr.heneria.zombie.core.economy.TransactionType;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Groups rapid transaction feedback and flushes it from the shared game tick. */
public final class PointDisplayService {
  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
  private final int windowTicks;

  public PointDisplayService(int windowTicks) {
    if (windowTicks < 1) {
      throw new IllegalArgumentException("windowTicks must be positive");
    }
    this.windowTicks = windowTicks;
  }

  public void accept(EconomyEvent event) {
    if (event.type() != EconomyEvent.Type.TRANSACTION_COMPLETED
        || !(event.data() instanceof Transaction transaction)) {
      return;
    }
    long signed =
        transaction.type() == TransactionType.DEBIT
            ? -transaction.amount()
            : transaction.type() == TransactionType.ADJUSTMENT
                ? transaction.balanceAfter() - transaction.balanceBefore()
                : transaction.amount();
    pending.compute(
        transaction.playerId(),
        (ignored, current) ->
            current == null
                ? new Pending(signed, windowTicks)
                : new Pending(Math.addExact(current.amount(), signed), windowTicks));
  }

  public void tick() {
    pending.replaceAll(
        (ignored, value) -> new Pending(value.amount(), Math.max(0, value.remainingTicks() - 1)));
    pending
        .entrySet()
        .removeIf(
            entry -> {
              if (entry.getValue().remainingTicks() > 0) {
                return false;
              }
              Player player = Bukkit.getPlayer(entry.getKey());
              if (player != null && entry.getValue().amount() != 0) {
                long amount = entry.getValue().amount();
                player.sendActionBar(
                    MINI.deserialize(
                        amount > 0
                            ? "<green>+" + amount + " points"
                            : "<red>" + amount + " points"));
              }
              return true;
            });
  }

  public void clear() {
    pending.clear();
  }

  private record Pending(long amount, int remainingTicks) {}
}
