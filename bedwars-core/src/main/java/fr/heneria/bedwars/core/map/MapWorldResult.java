package fr.heneria.bedwars.core.map;

/** Platform world operation result; Bukkit exceptions are converted to safe details. */
public record MapWorldResult(boolean successful, String detail) {
  public static MapWorldResult success() {
    return new MapWorldResult(true, "");
  }

  public static MapWorldResult failure(String detail) {
    return new MapWorldResult(false, detail == null ? "unknown" : detail);
  }
}
