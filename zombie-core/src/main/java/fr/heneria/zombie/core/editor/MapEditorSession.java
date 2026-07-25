package fr.heneria.zombie.core.editor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One administrator's isolated editing state and immutable map working copy. */
public final class MapEditorSession {
  private final UUID playerId;
  private final String mapId;
  private final UndoManager history = new UndoManager();
  private MapDefinition definition;
  private EditorTool tool = EditorTool.NONE;
  private Selection selection = Selection.EMPTY;
  private Clipboard clipboard = Clipboard.EMPTY;
  private String selectedZone = "";
  private MapObjectType selectedObjectType = MapObjectType.BARRICADE;
  private Instant lastActivity;

  public MapEditorSession(UUID playerId, MapDefinition definition, Instant now) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.definition = Objects.requireNonNull(definition, "definition");
    mapId = definition.id();
    lastActivity = Objects.requireNonNull(now, "now");
  }

  public UUID playerId() {
    return playerId;
  }

  public String mapId() {
    return mapId;
  }

  public synchronized MapDefinition definition() {
    return definition;
  }

  public synchronized void change(MapDefinition changed, Instant now) {
    if (!changed.id().equals(mapId)) {
      throw new IllegalArgumentException("Cannot switch map inside an editor session");
    }
    history.record(definition);
    definition = changed;
    lastActivity = now;
  }

  public synchronized boolean undo(Instant now) {
    var previous = history.undo(definition);
    previous.ifPresent(value -> definition = value);
    lastActivity = now;
    return previous.isPresent();
  }

  public synchronized boolean redo(Instant now) {
    var next = history.redo(definition);
    next.ifPresent(value -> definition = value);
    lastActivity = now;
    return next.isPresent();
  }

  public EditorTool tool() {
    return tool;
  }

  public void tool(EditorTool value) {
    tool = Objects.requireNonNull(value, "value");
  }

  public Selection selection() {
    return selection;
  }

  public void selection(Selection value) {
    selection = Objects.requireNonNull(value, "value");
  }

  public Clipboard clipboard() {
    return clipboard;
  }

  public void clipboard(Clipboard value) {
    clipboard = Objects.requireNonNull(value, "value");
  }

  public String selectedZone() {
    return selectedZone;
  }

  public void selectedZone(String value) {
    selectedZone = Objects.requireNonNull(value, "value");
  }

  public MapObjectType selectedObjectType() {
    return selectedObjectType;
  }

  public void selectedObjectType(MapObjectType value) {
    selectedObjectType = Objects.requireNonNull(value, "value");
  }

  public Instant lastActivity() {
    return lastActivity;
  }

  public int historySize() {
    return history.undoSize();
  }
}
