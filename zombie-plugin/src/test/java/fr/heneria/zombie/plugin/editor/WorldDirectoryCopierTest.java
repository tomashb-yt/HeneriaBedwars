package fr.heneria.zombie.plugin.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WorldDirectoryCopierTest {

  @Test
  void copiesAnImmutableWorldWithoutRuntimeIdentityFilesAndDeletesOnlyOwnedTarget()
      throws Exception {
    Path testRoot = Path.of("build", "tmp", "world-directory-copier-tests");
    Files.createDirectories(testRoot);
    Path temporary = Files.createTempDirectory(testRoot, "copy-");
    Path source = temporary.resolve("source");
    Path region = source.resolve("region");
    Files.createDirectories(region);
    Files.writeString(source.resolve("level.dat"), "level");
    Files.writeString(source.resolve("uid.dat"), "identity");
    Files.writeString(source.resolve("session.lock"), "lock");
    Files.writeString(region.resolve("r.0.0.mca"), "region");

    Path ownedRoot = temporary.resolve("zombie_editing");
    Path target = ownedRoot.resolve("hz_edit_crypt");
    WorldDirectoryCopier.replace(source, target);

    assertEquals("level", Files.readString(target.resolve("level.dat")));
    assertEquals("region", Files.readString(target.resolve("region/r.0.0.mca")));
    assertFalse(Files.exists(target.resolve("uid.dat")));
    assertFalse(Files.exists(target.resolve("session.lock")));
    assertTrue(Files.exists(source.resolve("uid.dat")));

    WorldDirectoryCopier.deleteOwned(target, ownedRoot);

    assertFalse(Files.exists(target));
    assertTrue(Files.exists(source.resolve("level.dat")));

    WorldDirectoryCopier.deleteOwned(source, temporary);
    WorldDirectoryCopier.deleteOwned(temporary, testRoot);
  }
}
