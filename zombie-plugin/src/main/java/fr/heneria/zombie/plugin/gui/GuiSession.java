package fr.heneria.zombie.plugin.gui;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Independent mutable navigation state owned by one online player. */
public final class GuiSession {

  private final UUID playerId;
  private final GuiNavigationHistory history = new GuiNavigationHistory();
  private final Map<String, String> filters = new HashMap<>();
  private final Map<String, Object> temporaryData = new HashMap<>();
  private UUID viewToken = UUID.randomUUID();
  private GuiId currentGui;
  private GuiContext currentContext = GuiContext.EMPTY;
  private GuiId homeGui;
  private int page;
  private String search = "";
  private GuiConfirmation confirmation;
  private GuiInputRequest inputRequest;
  private Instant lastActivity;
  private Instant lastClick = Instant.MIN;
  private long nextRefreshTick;

  /**
   * Creates a session.
   *
   * @param playerId owner
   * @param now creation instant
   */
  public GuiSession(UUID playerId, Instant now) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.lastActivity = Objects.requireNonNull(now, "now");
  }

  /**
   * @return owner
   */
  public UUID playerId() {
    return playerId;
  }

  /**
   * @return current view token
   */
  public UUID viewToken() {
    return viewToken;
  }

  /**
   * @return current GUI
   */
  public Optional<GuiId> currentGui() {
    return Optional.ofNullable(currentGui);
  }

  /**
   * @return current immutable context
   */
  public GuiContext currentContext() {
    return currentContext;
  }

  /**
   * @return configured home GUI
   */
  public Optional<GuiId> homeGui() {
    return Optional.ofNullable(homeGui);
  }

  /**
   * Changes the active screen and rotates its anti-stale token.
   *
   * @param gui new GUI
   * @param context context
   * @param home whether this becomes session home
   */
  public void activate(GuiId gui, GuiContext context, boolean home) {
    currentGui = Objects.requireNonNull(gui, "gui");
    currentContext = Objects.requireNonNull(context, "context");
    viewToken = UUID.randomUUID();
    page = 0;
    if (home || homeGui == null) {
      homeGui = gui;
    }
  }

  /**
   * @return navigation history
   */
  public GuiNavigationHistory history() {
    return history;
  }

  /**
   * @return zero-based current page
   */
  public int page() {
    return page;
  }

  /**
   * Sets a non-negative page.
   *
   * @param page page
   */
  public void page(int page) {
    this.page = Math.max(0, page);
  }

  /**
   * @return current normalized search
   */
  public String search() {
    return search;
  }

  /**
   * Changes current search.
   *
   * @param search search text
   */
  public void search(String search) {
    this.search = Objects.requireNonNull(search, "search").strip();
    page = 0;
  }

  /**
   * @return mutable session-local filters
   */
  public Map<String, String> filters() {
    return filters;
  }

  /**
   * @return mutable session-local temporary data
   */
  public Map<String, Object> temporaryData() {
    return temporaryData;
  }

  /**
   * @return pending confirmation
   */
  public Optional<GuiConfirmation> confirmation() {
    return Optional.ofNullable(confirmation);
  }

  /**
   * Changes pending confirmation.
   *
   * @param confirmation confirmation, or null
   */
  public void confirmation(GuiConfirmation confirmation) {
    this.confirmation = confirmation;
  }

  /**
   * @return pending input
   */
  public Optional<GuiInputRequest> inputRequest() {
    return Optional.ofNullable(inputRequest);
  }

  /**
   * Changes pending input.
   *
   * @param inputRequest request, or null
   */
  public void inputRequest(GuiInputRequest inputRequest) {
    this.inputRequest = inputRequest;
  }

  /**
   * @return last activity instant
   */
  public Instant lastActivity() {
    return lastActivity;
  }

  /**
   * Marks activity.
   *
   * @param now current time
   */
  public void touch(Instant now) {
    lastActivity = Objects.requireNonNull(now, "now");
  }

  /**
   * Atomically rejects accidental double clicks inside a short debounce window.
   *
   * @param now current instant
   * @param debounce minimum interval
   * @return whether this click may execute
   */
  public boolean tryClick(Instant now, java.time.Duration debounce) {
    Objects.requireNonNull(now, "now");
    Objects.requireNonNull(debounce, "debounce");
    if (now.isBefore(lastClick.plus(debounce))) {
      return false;
    }
    lastClick = now;
    return true;
  }

  /**
   * @return next scheduled refresh tick
   */
  public long nextRefreshTick() {
    return nextRefreshTick;
  }

  /**
   * Changes next scheduled refresh tick.
   *
   * @param tick absolute server tick
   */
  public void nextRefreshTick(long tick) {
    nextRefreshTick = tick;
  }

  /** Clears all transient references. */
  public void clear() {
    history.clear();
    filters.clear();
    temporaryData.clear();
    currentGui = null;
    confirmation = null;
    inputRequest = null;
  }
}
