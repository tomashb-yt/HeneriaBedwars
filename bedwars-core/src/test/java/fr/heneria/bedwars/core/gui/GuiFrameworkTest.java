package fr.heneria.bedwars.core.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.heneria.bedwars.core.config.PlaceholderContext;
import fr.heneria.bedwars.core.config.TranslationKey;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GuiFrameworkTest {
  private static GuiButton button() {
    return GuiButton.builder().item(GuiItem.of("STONE", "Test")).build();
  }

  private static Gui gui(String id) {
    return Gui.builder().id(id).title(id).rows(3).button(0, button()).build();
  }

  @Test
  void buildsValidImmutableMenu() {
    AtomicReference<GuiCloseReason> reason = new AtomicReference<>();
    Gui result =
        Gui.builder()
            .id("demo")
            .title("Demo")
            .rows(6)
            .fillEmptySlots(true)
            .button(10, button())
            .onClose(context -> reason.set(context.reason()))
            .build();
    assertEquals(54, result.size());
    assertTrue(result.fillEmptySlots());
    assertEquals(1, result.buttons().size());
    assertThrows(UnsupportedOperationException.class, () -> result.buttons().put(1, button()));
    result.closed(new GuiSession(UUID.randomUUID(), result, 5), GuiCloseReason.PLAYER);
    assertEquals(GuiCloseReason.PLAYER, reason.get());
  }

  @Test
  void rejectsInvalidIdentityRowsAndSlots() {
    assertThrows(GuiBuildException.class, () -> Gui.builder().id(" ").title("x").rows(1).build());
    assertThrows(GuiBuildException.class, () -> Gui.builder().id("x").title("x").rows(0).build());
    assertThrows(GuiBuildException.class, () -> Gui.builder().id("x").title("x").rows(7).build());
    assertThrows(
        GuiBuildException.class,
        () -> Gui.builder().id("x").title("x").rows(1).button(-1, button()));
    assertThrows(
        GuiBuildException.class,
        () -> Gui.builder().id("x").title("x").rows(1).button(9, button()).build());
  }

  @Test
  void duplicateRequiresExplicitReplacement() {
    Gui.Builder builder = Gui.builder().id("x").title("x").rows(1).button(1, button());
    assertThrows(DuplicateGuiSlotException.class, () -> builder.button(1, button()));
    assertEquals(1, builder.replaceButton(1, button()).build().buttons().size());
  }

  @Test
  void routesExactAndGenericButtonActions() throws Exception {
    AtomicInteger count = new AtomicInteger();
    GuiButton result =
        GuiButton.builder()
            .item(GuiItem.of("STONE", "x"))
            .onLeftClick(context -> count.addAndGet(1))
            .onRightClick(context -> count.addAndGet(10))
            .onClick(context -> count.addAndGet(100))
            .build();
    GuiClickContext context = context(gui("x"), GuiClickType.LEFT);
    result.action(GuiClickType.LEFT).orElseThrow().execute(context);
    result.action(GuiClickType.RIGHT).orElseThrow().execute(context);
    result.action(GuiClickType.MIDDLE).orElseThrow().execute(context);
    assertEquals(111, count.get());
  }

  @Test
  void exposesPermissionVisibilityAndEnabledConditions() {
    GuiSession session = new GuiSession(UUID.randomUUID(), gui("x"), 2);
    GuiRenderContext context = new GuiRenderContext("Alex", session);
    GuiButton result =
        GuiButton.builder()
            .item(GuiItem.of("STONE", "x"))
            .permission("test.use")
            .visibleWhen(value -> false)
            .enabledWhen(value -> false)
            .build();
    assertEquals("test.use", result.permission().orElseThrow());
    assertFalse(result.visible(context));
    assertFalse(result.enabled(context));
  }

  @Test
  void navigationSupportsBackRootLimitAndReplacement() {
    Gui root = gui("root");
    GuiSession session = new GuiSession(UUID.randomUUID(), root, 2);
    session.navigate(gui("one"), true);
    session.navigate(gui("two"), true);
    session.navigate(gui("three"), true);
    assertEquals(2, session.historySize());
    assertEquals("two", session.back().orElseThrow().id());
    session.navigate(gui("replacement"), false);
    assertEquals(1, session.historySize());
    assertEquals("root", session.goRoot().id());
    assertEquals(0, session.historySize());
  }

  @Test
  void oldViewCloseCannotDeleteNewView() {
    GuiSessionManager manager = new GuiSessionManager();
    UUID player = UUID.randomUUID();
    GuiSession session = manager.create(player, gui("a"), 5);
    UUID oldView = session.viewId();
    session.navigate(gui("b"), true);
    assertFalse(manager.remove(player, session.sessionId(), oldView));
    assertTrue(manager.find(player).isPresent());
    assertNotEquals(oldView, session.viewId());
  }

  @Test
  void oldSessionCloseCannotDeleteReplacementSession() {
    GuiSessionManager manager = new GuiSessionManager();
    UUID player = UUID.randomUUID();
    GuiSession old = manager.create(player, gui("a"), 5);
    GuiSession replacement = manager.create(player, gui("b"), 5);
    assertFalse(manager.remove(player, old.sessionId(), old.viewId()));
    assertEquals(replacement.sessionId(), manager.find(player).orElseThrow().sessionId());
    manager.clear();
    assertEquals(0, manager.size());
  }

  @Test
  void paginationHandlesEmptySingleMultipleAndShrinkingLists() {
    Pagination<Integer> empty = new Pagination<>(List.of(), 10);
    assertEquals(1, empty.pageCount());
    assertTrue(empty.currentItems().isEmpty());
    Pagination<Integer> pages =
        new Pagination<>(java.util.stream.IntStream.range(0, 25).boxed().toList(), 10);
    assertEquals(3, pages.pageCount());
    pages.next();
    pages.last();
    assertEquals(2, pages.page());
    assertEquals(5, pages.currentItems().size());
    pages.previous();
    assertEquals(1, pages.page());
    pages.items(List.of(1, 2));
    assertEquals(0, pages.page());
    pages.first();
  }

  @Test
  void rectangleProducesValidatedSlots() {
    assertEquals(List.of(10, 11, 12, 19, 20, 21), GuiSlots.rectangle(1, 1, 2, 3));
    assertThrows(IllegalArgumentException.class, () -> GuiSlots.rectangle(5, 8, 2, 2));
  }

  @Test
  void timestampCooldownRejectsOnlyRapidSecondClick() {
    GuiSession session = new GuiSession(UUID.randomUUID(), gui("x"), 1);
    assertTrue(session.acceptClick("button", 150, 1_000));
    assertFalse(session.acceptClick("button", 150, 1_100));
    assertTrue(session.acceptClick("button", 150, 1_151));
  }

  @Test
  void actionExecutorInterceptsFailureAndPreservesSession() {
    AtomicBoolean handled = new AtomicBoolean();
    GuiActionExecutor executor = new GuiActionExecutor((context, error) -> handled.set(true));
    Gui menu = gui("x");
    GuiClickContext context = context(menu, GuiClickType.LEFT);
    assertFalse(
        executor.execute(
            value -> {
              throw new IllegalStateException("controlled");
            },
            context));
    assertTrue(handled.get());
    assertEquals("x", context.session().current().id());
  }

  @Test
  void confirmationRequiresActionAndExecutesIt() throws Exception {
    assertThrows(GuiBuildException.class, () -> ConfirmationGui.builder().title("Confirm").build());
    AtomicBoolean confirmed = new AtomicBoolean();
    Gui result =
        ConfirmationGui.builder()
            .title("Confirm")
            .onConfirm(context -> confirmed.set(true))
            .build();
    result
        .buttons()
        .get(11)
        .action(GuiClickType.LEFT)
        .orElseThrow()
        .execute(context(result, GuiClickType.LEFT));
    assertTrue(confirmed.get());
  }

  private static GuiClickContext context(Gui gui, GuiClickType type) {
    GuiSession session = new GuiSession(UUID.randomUUID(), gui, 5);
    return new GuiClickContext(new FakeRuntime(), gui, session, 0, type, Map.of());
  }

  private static final class FakeRuntime implements GuiRuntime {
    public UUID playerId() {
      return UUID.randomUUID();
    }

    public String playerName() {
      return "Alex";
    }

    public boolean hasPermission(String permission) {
      return true;
    }

    public void open(Gui gui) {}

    public void replace(Gui gui) {}

    public void back() {}

    public void root() {}

    public void refresh() {}

    public void close() {}

    public void playSound(String id) {}

    public void message(TranslationKey key, PlaceholderContext placeholders) {}
  }
}
