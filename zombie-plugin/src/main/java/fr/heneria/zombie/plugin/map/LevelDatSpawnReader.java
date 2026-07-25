package fr.heneria.zombie.plugin.map;

import fr.heneria.zombie.core.map.MapTemplateDefinition.MapSpawn;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/** Reads only the world spawn from a compressed vanilla {@code level.dat}. */
final class LevelDatSpawnReader {

  private static final int MAX_DEPTH = 64;
  private static final int MAX_COLLECTION_LENGTH = 16_777_216;

  private LevelDatSpawnReader() {}

  /**
   * Reads the spawn stored under the root {@code Data} compound.
   *
   * @param levelDat compressed level data
   * @return detected spawn
   * @throws IOException when the file is malformed or has no complete spawn
   */
  static MapSpawn read(Path levelDat) throws IOException {
    Objects.requireNonNull(levelDat, "levelDat");
    SpawnAccumulator spawn = new SpawnAccumulator();
    try (DataInputStream input =
        new DataInputStream(
            new BufferedInputStream(new GZIPInputStream(Files.newInputStream(levelDat))))) {
      int rootType = input.readUnsignedByte();
      if (rootType != 10) {
        throw new IOException("level.dat root is not an NBT compound");
      }
      readNbtString(input);
      readCompound(input, "", 0, spawn);
    }
    if (!spawn.complete()) {
      throw new IOException("level.dat does not contain SpawnX, SpawnY and SpawnZ");
    }
    return new MapSpawn(spawn.x + 0.5D, spawn.y, spawn.z + 0.5D, spawn.yaw, 0.0F);
  }

  private static void readCompound(
      DataInputStream input, String path, int depth, SpawnAccumulator spawn) throws IOException {
    requireDepth(depth);
    while (!spawn.complete()) {
      int type = input.readUnsignedByte();
      if (type == 0) {
        return;
      }
      String name = readNbtString(input);
      String childPath = path.isEmpty() ? name : path + '.' + name;
      if (childPath.equals("Data.SpawnX") && type == 3) {
        spawn.x = input.readInt();
        spawn.hasX = true;
      } else if (childPath.equals("Data.SpawnY") && type == 3) {
        spawn.y = input.readInt();
        spawn.hasY = true;
      } else if (childPath.equals("Data.SpawnZ") && type == 3) {
        spawn.z = input.readInt();
        spawn.hasZ = true;
      } else if (childPath.equals("Data.SpawnAngle") && type == 5) {
        spawn.yaw = input.readFloat();
      } else {
        readPayload(input, type, childPath, depth + 1, spawn);
      }
    }
  }

  private static void readPayload(
      DataInputStream input, int type, String path, int depth, SpawnAccumulator spawn)
      throws IOException {
    requireDepth(depth);
    switch (type) {
      case 1 -> input.readByte();
      case 2 -> input.readShort();
      case 3 -> input.readInt();
      case 4 -> input.readLong();
      case 5 -> input.readFloat();
      case 6 -> input.readDouble();
      case 7 -> input.skipNBytes(collectionLength(input) * (long) Byte.BYTES);
      case 8 -> readNbtString(input);
      case 9 -> {
        int elementType = input.readUnsignedByte();
        int length = collectionLength(input);
        for (int index = 0; index < length && !spawn.complete(); index++) {
          readPayload(input, elementType, path, depth + 1, spawn);
        }
      }
      case 10 -> readCompound(input, path, depth + 1, spawn);
      case 11 -> input.skipNBytes(collectionLength(input) * (long) Integer.BYTES);
      case 12 -> input.skipNBytes(collectionLength(input) * (long) Long.BYTES);
      default -> throw new IOException("Unsupported NBT tag type " + type);
    }
  }

  private static int collectionLength(DataInputStream input) throws IOException {
    int length = input.readInt();
    if (length < 0 || length > MAX_COLLECTION_LENGTH) {
      throw new IOException("Unsafe NBT collection length " + length);
    }
    return length;
  }

  private static String readNbtString(DataInputStream input) throws IOException {
    int length = input.readUnsignedShort();
    byte[] encoded = input.readNBytes(length);
    if (encoded.length != length) {
      throw new IOException("Unexpected end of NBT string");
    }
    return new String(encoded, StandardCharsets.UTF_8);
  }

  private static void requireDepth(int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new IOException("NBT nesting exceeds " + MAX_DEPTH);
    }
  }

  private static final class SpawnAccumulator {
    private int x;
    private int y;
    private int z;
    private float yaw;
    private boolean hasX;
    private boolean hasY;
    private boolean hasZ;

    private boolean complete() {
      return hasX && hasY && hasZ;
    }
  }
}
