package fr.heneria.zombie.core.editor;

import java.util.Map;

/** Session-local immutable clipboard for duplicated editor entities. */
public record Clipboard(String kind, Map<String, String> values) {
  public static final Clipboard EMPTY = new Clipboard("", Map.of());

  public Clipboard {
    kind = kind == null ? "" : kind;
    values = Map.copyOf(values);
  }
}
