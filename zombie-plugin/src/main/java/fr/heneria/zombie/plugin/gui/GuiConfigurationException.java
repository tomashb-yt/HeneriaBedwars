package fr.heneria.zombie.plugin.gui;

import java.util.List;

/** Aggregated rejection of an invalid GUI YAML candidate. */
@SuppressWarnings("serial")
public final class GuiConfigurationException extends Exception {

  private static final long serialVersionUID = 1L;
  private final List<String> errors;

  /**
   * Creates the exception.
   *
   * @param errors validation errors
   */
  public GuiConfigurationException(List<String> errors) {
    super(String.join("; ", errors));
    this.errors = List.copyOf(errors);
  }

  /**
   * @return immutable errors
   */
  public List<String> errors() {
    return errors;
  }
}
