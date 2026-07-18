package fr.heneria.bedwars.core.game.shop;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable, deterministically ordered collection of purchasable offers. */
public final class ShopCatalog {
  private final Map<ShopOfferId, ShopOffer> offers;

  public ShopCatalog(List<ShopOffer> values) {
    LinkedHashMap<ShopOfferId, ShopOffer> indexed = new LinkedHashMap<>();
    values.stream()
        .sorted(Comparator.comparingInt(ShopOffer::order).thenComparing(ShopOffer::id))
        .forEach(
            offer -> {
              if (indexed.putIfAbsent(offer.id(), offer) != null)
                throw new IllegalArgumentException("Duplicate shop offer " + offer.id().value());
            });
    offers = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
  }

  public Optional<ShopOffer> find(ShopOfferId id) {
    return Optional.ofNullable(offers.get(id));
  }

  public List<ShopOffer> category(ShopCategory category) {
    return offers.values().stream().filter(offer -> offer.category() == category).toList();
  }

  public List<ShopOffer> all() {
    return List.copyOf(offers.values());
  }

  public int size() {
    return offers.size();
  }
}
