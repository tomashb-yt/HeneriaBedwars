package fr.heneria.bedwars.core.gui;

import java.time.Duration;
import java.util.Objects;

/** Reusable immutable confirmation menu factory with guarded confirm and cancel actions. */
public final class ConfirmationGui {
  private ConfirmationGui() {}

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String id = "confirmation";
    private String title;
    private GuiItem information = GuiItem.of("PAPER", "Information");
    private GuiItem confirm = GuiItem.of("LIME_CONCRETE", "Confirm");
    private GuiItem cancel = GuiItem.of("RED_CONCRETE", "Cancel");
    private GuiAction onConfirm;
    private GuiAction onCancel = GuiClickContext::back;
    private String permission;
    private Duration cooldown = Duration.ofMillis(500);

    public Builder id(String value) {
      id = value;
      return this;
    }

    public Builder title(String value) {
      title = value;
      return this;
    }

    public Builder information(GuiItem value) {
      information = value;
      return this;
    }

    public Builder confirmItem(GuiItem value) {
      confirm = value;
      return this;
    }

    public Builder cancelItem(GuiItem value) {
      cancel = value;
      return this;
    }

    public Builder onConfirm(GuiAction value) {
      onConfirm = Objects.requireNonNull(value);
      return this;
    }

    public Builder onCancel(GuiAction value) {
      onCancel = Objects.requireNonNull(value);
      return this;
    }

    public Builder permission(String value) {
      permission = value;
      return this;
    }

    public Builder cooldown(Duration value) {
      cooldown = value;
      return this;
    }

    public Gui build() {
      if (onConfirm == null) throw new GuiBuildException("confirmation action is required");
      GuiButton.Builder confirmButton =
          GuiButton.builder().item(confirm).cooldown(cooldown).onLeftClick(onConfirm);
      if (permission != null) confirmButton.permission(permission);
      return Gui.builder()
          .id(id)
          .title(Objects.requireNonNull(title, "title"))
          .rows(3)
          .fillEmptySlots(true)
          .button(13, GuiButton.builder().item(information).build())
          .button(11, confirmButton.build())
          .button(
              15, GuiButton.builder().item(cancel).cooldown(cooldown).onLeftClick(onCancel).build())
          .build();
    }
  }
}
