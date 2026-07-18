package fr.heneria.bedwars.core.game.equipment;

/** Match-scoped permanent armor and degradable tool progression. */
public final class PlayerEquipment {
  private int armorTier;
  private int pickaxeTier;
  private int axeTier;
  private boolean shears;

  public synchronized EquipmentPurchaseCode canPurchase(EquipmentKind kind, int tier) {
    return switch (kind) {
      case ITEM -> EquipmentPurchaseCode.AVAILABLE;
      case ARMOR ->
          tier <= armorTier
              ? EquipmentPurchaseCode.ALREADY_OWNED
              : tier <= 3 ? EquipmentPurchaseCode.AVAILABLE : EquipmentPurchaseCode.WRONG_TIER;
      case PICKAXE -> progression(pickaxeTier, tier, 4);
      case AXE -> progression(axeTier, tier, 4);
      case SHEARS ->
          shears
              ? EquipmentPurchaseCode.ALREADY_OWNED
              : tier == 1 ? EquipmentPurchaseCode.AVAILABLE : EquipmentPurchaseCode.WRONG_TIER;
    };
  }

  public synchronized boolean purchase(EquipmentKind kind, int tier) {
    if (canPurchase(kind, tier) != EquipmentPurchaseCode.AVAILABLE) return false;
    switch (kind) {
      case ITEM -> {
        return true;
      }
      case ARMOR -> armorTier = tier;
      case PICKAXE -> pickaxeTier = tier;
      case AXE -> axeTier = tier;
      case SHEARS -> shears = true;
    }
    return true;
  }

  /** BedWars tools lose one tier on death; permanent armor and shears are retained. */
  public synchronized void onDeath() {
    pickaxeTier = Math.max(0, pickaxeTier - 1);
    axeTier = Math.max(0, axeTier - 1);
  }

  public synchronized PlayerEquipmentSnapshot snapshot() {
    return new PlayerEquipmentSnapshot(armorTier, pickaxeTier, axeTier, shears);
  }

  private static EquipmentPurchaseCode progression(int current, int requested, int maximum) {
    if (requested <= current) return EquipmentPurchaseCode.ALREADY_OWNED;
    return requested == current + 1 && requested <= maximum
        ? EquipmentPurchaseCode.AVAILABLE
        : EquipmentPurchaseCode.WRONG_TIER;
  }
}
