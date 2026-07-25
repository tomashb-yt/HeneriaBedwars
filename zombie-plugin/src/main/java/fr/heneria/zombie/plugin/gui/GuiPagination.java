package fr.heneria.zombie.plugin.gui;

import java.util.List;

/** Stateless pagination helpers without an artificial total-item limit. */
public final class GuiPagination {

  private GuiPagination() {}

  /**
   * Calculates the number of pages.
   *
   * @param itemCount total items
   * @param pageSize positive page size
   * @return at least one page
   */
  public static int pageCount(int itemCount, int pageSize) {
    if (itemCount < 0 || pageSize <= 0) {
      throw new IllegalArgumentException("Invalid pagination dimensions");
    }
    return Math.max(1, Math.ceilDiv(itemCount, pageSize));
  }

  /**
   * Returns one immutable page.
   *
   * @param items source list
   * @param requestedPage zero-based page
   * @param pageSize positive page size
   * @param <T> item type
   * @return page slice
   */
  public static <T> Page<T> page(List<T> items, int requestedPage, int pageSize) {
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be positive");
    }
    int pages = pageCount(items.size(), pageSize);
    int page = Math.clamp(requestedPage, 0, pages - 1);
    int from = Math.min(items.size(), page * pageSize);
    int to = Math.min(items.size(), from + pageSize);
    return new Page<>(List.copyOf(items.subList(from, to)), page, pages, items.size());
  }

  /** Immutable page result. */
  public record Page<T>(List<T> items, int index, int pageCount, int totalItems) {}
}
