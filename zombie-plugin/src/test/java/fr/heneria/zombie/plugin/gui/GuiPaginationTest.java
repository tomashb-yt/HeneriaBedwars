package fr.heneria.zombie.plugin.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GuiPaginationTest {

  @Test
  void paginatesAnUnboundedCollectionAndClampsRequestedPage() {
    var source = IntStream.range(0, 1_003).boxed().toList();
    var page = GuiPagination.page(source, 99, 28);

    assertEquals(36, page.pageCount());
    assertEquals(35, page.index());
    assertEquals(23, page.items().size());
    assertEquals(980, page.items().getFirst());
  }

  @Test
  void emptyCollectionStillHasOneEmptyPage() {
    var page = GuiPagination.page(java.util.List.of(), 4, 28);
    assertEquals(1, page.pageCount());
    assertEquals(0, page.index());
    assertFalse(page.items().iterator().hasNext());
  }

  @Test
  void rejectsInvalidPageSize() {
    assertThrows(
        IllegalArgumentException.class, () -> GuiPagination.page(java.util.List.of("map"), 0, 0));
  }
}
