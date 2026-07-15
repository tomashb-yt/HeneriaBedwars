package fr.heneria.bedwars.core.gui;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Immutable menu definition. A platform creates a distinct view for every opening. */
public final class Gui {
  private final String id;
  private final String title;
  private final int rows;
  private final Map<Integer, GuiButton> buttons;
  private final boolean fillEmptySlots;
  private final GuiItem filler;
  private final Consumer<GuiSession> onOpen;
  private final Consumer<GuiCloseContext> onClose;
  private final Duration autoRefresh;
  private final Map<String, Object> data;

  private Gui(Builder builder) {
    id = builder.id;
    title = builder.title;
    rows = builder.rows;
    buttons = Map.copyOf(builder.buttons);
    fillEmptySlots = builder.fill;
    filler = builder.filler;
    onOpen = builder.onOpen;
    onClose = builder.onClose;
    autoRefresh = builder.autoRefresh;
    data = Map.copyOf(builder.data);
  }

  public static Builder builder() {
    return new Builder();
  }

  public String id() {
    return id;
  }

  public String title() {
    return title;
  }

  public int rows() {
    return rows;
  }

  public int size() {
    return rows * 9;
  }

  public Map<Integer, GuiButton> buttons() {
    return buttons;
  }

  public boolean fillEmptySlots() {
    return fillEmptySlots;
  }

  public GuiItem filler() {
    return filler;
  }

  public Optional<Duration> autoRefresh() {
    return Optional.ofNullable(autoRefresh);
  }

  public Map<String, Object> data() {
    return data;
  }

  public void opened(GuiSession session) {
    onOpen.accept(session);
  }

  public void closed(GuiSession session, GuiCloseReason reason) {
    onClose.accept(new GuiCloseContext(session, this, reason));
  }

  /** Validating builder; duplicate slots require {@link #replaceButton(int, GuiButton)}. */
  public static final class Builder {
    private String id;
    private String title;
    private int rows;
    private final Map<Integer, GuiButton> buttons = new LinkedHashMap<>();
    private boolean fill;
    private GuiItem filler = GuiItem.of("GRAY_STAINED_GLASS_PANE", " ");
    private Consumer<GuiSession> onOpen = session -> {};
    private Consumer<GuiCloseContext> onClose = context -> {};
    private Duration autoRefresh;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public Builder id(String value) {
      id = value;
      return this;
    }

    public Builder title(String value) {
      title = value;
      return this;
    }

    public Builder rows(int value) {
      rows = value;
      return this;
    }

    public Builder fillEmptySlots(boolean value) {
      fill = value;
      return this;
    }

    public Builder filler(GuiItem value) {
      filler = Objects.requireNonNull(value);
      return this;
    }

    public Builder button(int slot, GuiButton button) {
      validateSlot(slot);
      if (buttons.putIfAbsent(slot, button) != null) throw new DuplicateGuiSlotException(slot);
      return this;
    }

    public Builder replaceButton(int slot, GuiButton button) {
      validateSlot(slot);
      buttons.put(slot, button);
      return this;
    }

    public Builder onOpen(Consumer<GuiSession> value) {
      onOpen = Objects.requireNonNull(value);
      return this;
    }

    public Builder onClose(Consumer<GuiCloseContext> value) {
      onClose = Objects.requireNonNull(value);
      return this;
    }

    public Builder autoRefresh(Duration value) {
      if (value.isNegative() || value.isZero())
        throw new GuiBuildException("auto refresh must be positive");
      autoRefresh = value;
      return this;
    }

    public Builder data(String key, Object value) {
      data.put(key, value);
      return this;
    }

    public Gui build() {
      if (id == null || id.isBlank())
        throw new GuiBuildException("GUI identifier must not be blank");
      if (title == null) throw new GuiBuildException("GUI title must not be null");
      if (rows < 1 || rows > 6) throw new GuiBuildException("GUI rows must be between 1 and 6");
      for (int slot : buttons.keySet())
        if (slot >= rows * 9) throw new GuiBuildException("GUI slot outside final size: " + slot);
      return new Gui(this);
    }

    private static void validateSlot(int slot) {
      if (slot < 0 || slot >= 54)
        throw new GuiBuildException("GUI slot must be between 0 and 53: " + slot);
    }
  }
}
