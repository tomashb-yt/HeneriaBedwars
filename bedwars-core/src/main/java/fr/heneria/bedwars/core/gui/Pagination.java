package fr.heneria.bedwars.core.gui;

import java.util.List;

/** Generic zero-based pagination that clamps disappearing pages safely. */
public final class Pagination<T> {
  private List<T> items;
  private final int pageSize;
  private int page;

  public Pagination(List<T> items, int pageSize) {
    if (pageSize < 1) throw new IllegalArgumentException("pageSize must be positive");
    this.items = List.copyOf(items);
    this.pageSize = pageSize;
  }

  public int page() {
    return page;
  }

  public int pageCount() {
    return Math.max(1, (items.size() + pageSize - 1) / pageSize);
  }

  public List<T> currentItems() {
    int from = Math.min(page * pageSize, items.size());
    return items.subList(from, Math.min(from + pageSize, items.size()));
  }

  public void first() {
    page = 0;
  }

  public void last() {
    page = pageCount() - 1;
  }

  public void next() {
    page = Math.min(page + 1, pageCount() - 1);
  }

  public void previous() {
    page = Math.max(0, page - 1);
  }

  public void page(int value) {
    page = Math.max(0, Math.min(value, pageCount() - 1));
  }

  public void items(List<T> values) {
    items = List.copyOf(values);
    page(page);
  }
}
