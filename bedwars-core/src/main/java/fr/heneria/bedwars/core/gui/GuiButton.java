package fr.heneria.bedwars.core.gui;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/** Immutable button with dynamic rendering, conditions, permissions and click routing. */
public final class GuiButton {
  private final Function<GuiRenderContext, GuiItem> item;
  private final Function<GuiRenderContext, String> itemKey;
  private final Function<GuiRenderContext, Map<String, ?>> itemPlaceholders;
  private final Map<GuiClickType, GuiAction> actions;
  private final GuiAction genericAction;
  private final Predicate<GuiRenderContext> visible;
  private final Predicate<GuiRenderContext> enabled;
  private final String permission;
  private final long cooldownMillis;

  private GuiButton(Builder builder) {
    item = builder.item;
    itemKey = builder.itemKey;
    itemPlaceholders = builder.itemPlaceholders;
    actions = Map.copyOf(builder.actions);
    genericAction = builder.genericAction;
    visible = builder.visible;
    enabled = builder.enabled;
    permission = builder.permission;
    cooldownMillis = builder.cooldownMillis;
  }

  public static Builder builder() {
    return new Builder();
  }

  public GuiItem render(GuiRenderContext context) {
    if (item == null) throw new GuiBuildException("button uses an item key, not a GuiItem");
    return item.apply(context);
  }

  public Optional<String> itemKey(GuiRenderContext context) {
    return itemKey == null ? Optional.empty() : Optional.ofNullable(itemKey.apply(context));
  }

  public Map<String, ?> itemPlaceholders(GuiRenderContext context) {
    return Map.copyOf(itemPlaceholders.apply(context));
  }

  public boolean visible(GuiRenderContext context) {
    return visible.test(context);
  }

  public boolean enabled(GuiRenderContext context) {
    return enabled.test(context);
  }

  public Optional<String> permission() {
    return Optional.ofNullable(permission);
  }

  public long cooldownMillis() {
    return cooldownMillis;
  }

  public Optional<GuiAction> action(GuiClickType type) {
    GuiAction specific = actions.get(type);
    if (specific != null) return Optional.of(specific);
    return switch (type) {
      case LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT, MIDDLE -> Optional.ofNullable(genericAction);
      default -> Optional.empty();
    };
  }

  public static final class Builder {
    private Function<GuiRenderContext, GuiItem> item;
    private Function<GuiRenderContext, String> itemKey;
    private Function<GuiRenderContext, Map<String, ?>> itemPlaceholders = context -> Map.of();
    private final Map<GuiClickType, GuiAction> actions = new EnumMap<>(GuiClickType.class);
    private GuiAction genericAction;
    private Predicate<GuiRenderContext> visible = context -> true;
    private Predicate<GuiRenderContext> enabled = context -> true;
    private String permission;
    private long cooldownMillis = -1;

    public Builder item(GuiItem value) {
      item = context -> value;
      return this;
    }

    public Builder item(Function<GuiRenderContext, GuiItem> value) {
      item = Objects.requireNonNull(value);
      return this;
    }

    public Builder itemKey(String value) {
      Objects.requireNonNull(value);
      itemKey = context -> value;
      return this;
    }

    public Builder itemKey(Function<GuiRenderContext, String> value) {
      itemKey = Objects.requireNonNull(value);
      return this;
    }

    public Builder itemPlaceholders(Function<GuiRenderContext, Map<String, ?>> value) {
      itemPlaceholders = Objects.requireNonNull(value);
      return this;
    }

    public Builder on(GuiClickType type, GuiAction action) {
      actions.put(type, action);
      return this;
    }

    public Builder onLeftClick(GuiAction action) {
      return on(GuiClickType.LEFT, action);
    }

    public Builder onRightClick(GuiAction action) {
      return on(GuiClickType.RIGHT, action);
    }

    public Builder onClick(GuiAction action) {
      genericAction = action;
      return this;
    }

    public Builder permission(String value) {
      permission = value;
      return this;
    }

    public Builder visibleWhen(Predicate<GuiRenderContext> value) {
      visible = Objects.requireNonNull(value);
      return this;
    }

    public Builder enabledWhen(Predicate<GuiRenderContext> value) {
      enabled = Objects.requireNonNull(value);
      return this;
    }

    public Builder cooldown(Duration value) {
      cooldownMillis = value.toMillis();
      return this;
    }

    public GuiButton build() {
      if ((item == null) == (itemKey == null))
        throw new GuiBuildException("button requires exactly one item source");
      return new GuiButton(this);
    }
  }
}
