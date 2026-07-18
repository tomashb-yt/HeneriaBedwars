package fr.heneria.bedwars.core.game.shop;

import java.util.Objects;

/** Purchase result suitable for GUI feedback without exposing platform objects. */
public record ShopPurchaseResult(ShopPurchaseCode code, ShopOffer offer, int balance) {
  public ShopPurchaseResult {
    code = Objects.requireNonNull(code, "code");
    offer = Objects.requireNonNull(offer, "offer");
    if (balance < 0) balance = 0;
  }

  public boolean successful() {
    return code == ShopPurchaseCode.SUCCESS;
  }
}
