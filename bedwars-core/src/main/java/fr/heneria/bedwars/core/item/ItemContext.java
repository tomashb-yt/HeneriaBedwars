package fr.heneria.bedwars.core.item;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable dynamic rendering context with no dependency on Bukkit or BedWars domain objects. */
public final class ItemContext {
  public static final ItemContext EMPTY = builder().build();
  private final UUID playerId;
  private final String playerName;
  private final String locale;
  private final String menuId;
  private final int page;
  private final Map<String, String> placeholders;

  private ItemContext(Builder builder) {
    playerId = builder.playerId;
    playerName = builder.playerName;
    locale = builder.locale;
    menuId = builder.menuId;
    page = builder.page;
    placeholders = Map.copyOf(builder.placeholders);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Optional<UUID> playerId() {
    return Optional.ofNullable(playerId);
  }

  public Optional<String> playerName() {
    return Optional.ofNullable(playerName);
  }

  public Optional<String> locale() {
    return Optional.ofNullable(locale);
  }

  public Optional<String> menuId() {
    return Optional.ofNullable(menuId);
  }

  public int page() {
    return page;
  }

  public Map<String, String> placeholders() {
    return placeholders;
  }

  public PlaceholderContext placeholderContext() {
    Map<String, String> values = new LinkedHashMap<>(placeholders);
    if (playerName != null) values.putIfAbsent("player", playerName);
    values.putIfAbsent("page", String.valueOf(page));
    if (menuId != null) values.putIfAbsent("menu", menuId);
    return new PlaceholderContext(values);
  }

  public static final class Builder {
    private UUID playerId;
    private String playerName;
    private String locale;
    private String menuId;
    private int page;
    private final Map<String, String> placeholders = new LinkedHashMap<>();

    public Builder player(UUID id, String name) {
      playerId = id;
      playerName = name;
      return this;
    }

    public Builder locale(String value) {
      locale = value;
      return this;
    }

    public Builder menu(String value) {
      menuId = value;
      return this;
    }

    public Builder page(int value) {
      page = Math.max(0, value);
      return this;
    }

    public Builder placeholder(String key, Object value) {
      placeholders.put(Objects.requireNonNull(key), value == null ? "" : String.valueOf(value));
      return this;
    }

    public Builder placeholders(Map<String, ?> values) {
      values.forEach(this::placeholder);
      return this;
    }

    public ItemContext build() {
      return new ItemContext(this);
    }
  }
}
