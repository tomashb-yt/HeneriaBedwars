package fr.heneria.bedwars.core.map;

import java.io.IOException;

/**
 * Heavy file-operation port.
 *
 * <p>{@link #duplicate} and backup/delete operations must run outside the server thread. Paths are
 * derived only from validated ids and must remain below configured roots.
 */
public interface MapFileService {
  void createTemplateDirectory(MapTemplate template) throws IOException;

  void duplicate(MapTemplate source, MapTemplate destination) throws IOException;

  void backup(MapTemplate template, String reason, String pluginVersion) throws IOException;

  void deleteTemplate(MapTemplate template) throws IOException;

  boolean templateExists(MapTemplate template);
}
