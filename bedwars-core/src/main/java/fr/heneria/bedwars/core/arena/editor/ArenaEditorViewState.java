package fr.heneria.bedwars.core.arena.editor;

import java.util.HashMap;
import java.util.Map;

/** Per-player editor navigation preferences and observed optimistic revisions. */
public final class ArenaEditorViewState {
  private ArenaListFilter filter = ArenaListFilter.ALL;
  private ArenaListSort sort = ArenaListSort.ID;
  private int page;
  private final Map<String, Long> observedRevisions = new HashMap<>();

  public ArenaListFilter filter() {
    return filter;
  }

  public void filter(ArenaListFilter value) {
    filter = value;
    page = 0;
  }

  public ArenaListSort sort() {
    return sort;
  }

  public void sort(ArenaListSort value) {
    sort = value;
    page = 0;
  }

  public int page() {
    return page;
  }

  public void page(int value) {
    page = Math.max(0, value);
  }

  public void observe(String arenaId, long revision) {
    observedRevisions.put(arenaId, revision);
  }

  public long observedRevision(String arenaId, long fallback) {
    return observedRevisions.getOrDefault(arenaId, fallback);
  }
}
