package fr.heneria.bedwars.core.map.operation;

public enum MapOperationStatus {
  PENDING,
  RUNNING,
  SUCCESS,
  FAILED,
  CANCELLED;

  public boolean active() {
    return this == PENDING || this == RUNNING;
  }
}
