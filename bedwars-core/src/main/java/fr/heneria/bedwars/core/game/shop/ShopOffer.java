package fr.heneria.bedwars.core.game.shop;

import fr.heneria.bedwars.core.game.equipment.EquipmentKind;
import java.util.Objects;

/** Immutable product and price captured from the active shop catalog. */
public record ShopOffer(
    ShopOfferId id,
    ShopCategory category,
    String material,
    int amount,
    ShopCurrency currency,
    int price,
    String translationKey,
    int order,
    EquipmentKind kind,
    int tier) {
  public ShopOffer {
    id = Objects.requireNonNull(id, "id");
    category = Objects.requireNonNull(category, "category");
    material = text(material, "material");
    currency = Objects.requireNonNull(currency, "currency");
    translationKey = text(translationKey, "translationKey");
    kind = Objects.requireNonNull(kind, "kind");
    if (amount < 1 || amount > 4096) throw new IllegalArgumentException("Invalid offer amount");
    if (price < 1 || price > 4096) throw new IllegalArgumentException("Invalid offer price");
    if (order < 0) throw new IllegalArgumentException("Offer order cannot be negative");
    if (kind == EquipmentKind.ITEM && tier != 0)
      throw new IllegalArgumentException("A regular item cannot define a tier");
    if (kind != EquipmentKind.ITEM && tier < 1)
      throw new IllegalArgumentException("Equipment tier must be positive");
  }

  public ShopOffer(
      ShopOfferId id,
      ShopCategory category,
      String material,
      int amount,
      ShopCurrency currency,
      int price,
      String translationKey,
      int order) {
    this(
        id,
        category,
        material,
        amount,
        currency,
        price,
        translationKey,
        order,
        EquipmentKind.ITEM,
        0);
  }

  private static String text(String value, String field) {
    String clean = Objects.requireNonNull(value, field).trim();
    if (clean.isEmpty()) throw new IllegalArgumentException(field + " is blank");
    return clean;
  }
}
