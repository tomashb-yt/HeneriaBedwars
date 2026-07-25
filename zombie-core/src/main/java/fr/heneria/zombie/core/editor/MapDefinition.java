package fr.heneria.zombie.core.editor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable, versioned and platform-neutral complete Zombies map definition. */
public record MapDefinition(
    int schemaVersion,
    String id,
    String displayName,
    String description,
    UUID creator,
    Instant createdAt,
    Instant updatedAt,
    String world,
    String icon,
    String image,
    int maximumPlayers,
    String music,
    String difficulty,
    String gameMode,
    Optional<MapPoint> playerSpawn,
    Map<String, Zone> zones,
    Map<String, Door> doors,
    Map<String, ZombieSpawn> zombieSpawns,
    Map<String, MapObject> objects) {

  public static final int CURRENT_SCHEMA = 2;

  public MapDefinition {
    if (schemaVersion != CURRENT_SCHEMA
        || !safeId(id)
        || displayName.isBlank()
        || world.isBlank()
        || maximumPlayers <= 0) {
      throw new IllegalArgumentException("Invalid map definition");
    }
    Objects.requireNonNull(description, "description");
    Objects.requireNonNull(creator, "creator");
    Objects.requireNonNull(createdAt, "createdAt");
    Objects.requireNonNull(updatedAt, "updatedAt");
    Objects.requireNonNull(icon, "icon");
    Objects.requireNonNull(image, "image");
    Objects.requireNonNull(music, "music");
    Objects.requireNonNull(difficulty, "difficulty");
    Objects.requireNonNull(gameMode, "gameMode");
    playerSpawn = Objects.requireNonNull(playerSpawn, "playerSpawn");
    zones = Map.copyOf(zones);
    doors = Map.copyOf(doors);
    zombieSpawns = Map.copyOf(zombieSpawns);
    objects = Map.copyOf(objects);
  }

  public static MapDefinition create(
      String id, String displayName, UUID creator, Instant now, String world) {
    return new MapDefinition(
        CURRENT_SCHEMA,
        id,
        displayName,
        "",
        creator,
        now,
        now,
        world,
        "FILLED_MAP",
        "",
        4,
        "",
        "NORMAL",
        "CLASSIC",
        Optional.empty(),
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of());
  }

  public MapDefinition withPlayerSpawn(MapPoint point, Instant now) {
    return copy(now, Optional.of(point), zones, doors, zombieSpawns, objects);
  }

  public MapDefinition withZone(Zone zone, Instant now) {
    LinkedHashMap<String, Zone> changed = new LinkedHashMap<>(zones);
    changed.put(zone.id(), zone);
    return copy(now, playerSpawn, changed, doors, zombieSpawns, objects);
  }

  public MapDefinition withDoor(Door door, Instant now) {
    LinkedHashMap<String, Door> changed = new LinkedHashMap<>(doors);
    changed.put(door.id(), door);
    return copy(now, playerSpawn, zones, changed, zombieSpawns, objects);
  }

  public MapDefinition withZombieSpawn(ZombieSpawn spawn, Instant now) {
    LinkedHashMap<String, ZombieSpawn> changed = new LinkedHashMap<>(zombieSpawns);
    changed.put(spawn.id(), spawn);
    return copy(now, playerSpawn, zones, doors, changed, objects);
  }

  public MapDefinition withObject(MapObject object, Instant now) {
    LinkedHashMap<String, MapObject> changed = new LinkedHashMap<>(objects);
    changed.put(object.id(), object);
    return copy(now, playerSpawn, zones, doors, zombieSpawns, changed);
  }

  public MapDefinition withDisplayName(String value, Instant now) {
    if (value == null || value.isBlank() || value.length() > 64) {
      throw new IllegalArgumentException("Invalid display name");
    }
    return new MapDefinition(
        schemaVersion,
        id,
        value,
        description,
        creator,
        createdAt,
        now,
        world,
        icon,
        image,
        maximumPlayers,
        music,
        difficulty,
        gameMode,
        playerSpawn,
        zones,
        doors,
        zombieSpawns,
        objects);
  }

  public MapDefinition withGeneralInformation(String field, String value, Instant now) {
    Objects.requireNonNull(value, "value");
    String newName = displayName;
    String newDescription = description;
    String newIcon = icon;
    String newImage = image;
    int newMaximumPlayers = maximumPlayers;
    String newMusic = music;
    String newDifficulty = difficulty;
    String newGameMode = gameMode;
    switch (field) {
      case "name" -> newName = required(value, 64, field);
      case "description" -> newDescription = limited(value, 512, field);
      case "icon" -> newIcon = required(value, 64, field).toUpperCase(java.util.Locale.ROOT);
      case "image" -> newImage = limited(value, 256, field);
      case "maximum-players" -> {
        try {
          newMaximumPlayers = Integer.parseInt(value);
        } catch (NumberFormatException failure) {
          throw new IllegalArgumentException("Invalid maximum players", failure);
        }
        if (newMaximumPlayers <= 0 || newMaximumPlayers > 1000) {
          throw new IllegalArgumentException("Maximum players must be between 1 and 1000");
        }
      }
      case "music" -> newMusic = limited(value, 128, field);
      case "difficulty" -> newDifficulty = required(value, 32, field);
      case "game-mode" -> newGameMode = required(value, 32, field);
      default -> throw new IllegalArgumentException("Unknown general field " + field);
    }
    return new MapDefinition(
        schemaVersion,
        id,
        newName,
        newDescription,
        creator,
        createdAt,
        now,
        world,
        newIcon,
        newImage,
        newMaximumPlayers,
        newMusic,
        newDifficulty,
        newGameMode,
        playerSpawn,
        zones,
        doors,
        zombieSpawns,
        objects);
  }

  public MapDefinition without(String kind, String entityId, Instant now) {
    LinkedHashMap<String, Zone> newZones = new LinkedHashMap<>(zones);
    LinkedHashMap<String, Door> newDoors = new LinkedHashMap<>(doors);
    LinkedHashMap<String, ZombieSpawn> newSpawns = new LinkedHashMap<>(zombieSpawns);
    LinkedHashMap<String, MapObject> newObjects = new LinkedHashMap<>(objects);
    switch (kind) {
      case "zone" -> newZones.remove(entityId);
      case "door" -> newDoors.remove(entityId);
      case "spawn" -> newSpawns.remove(entityId);
      case "object" -> newObjects.remove(entityId);
      default -> throw new IllegalArgumentException("Unknown entity kind " + kind);
    }
    return copy(now, playerSpawn, newZones, newDoors, newSpawns, newObjects);
  }

  public MapDefinition moved(String kind, String entityId, MapPoint point, Instant now) {
    return switch (kind) {
      case "zone" -> {
        Zone value = Objects.requireNonNull(zones.get(entityId), "zone");
        yield withZone(
            new Zone(
                value.id(),
                value.name(),
                value.color(),
                value.music(),
                value.ambience(),
                value.volume(),
                point),
            now);
      }
      case "door" -> {
        Door value = Objects.requireNonNull(doors.get(entityId), "door");
        yield withDoor(
            new Door(
                value.id(),
                value.name(),
                value.price(),
                value.sourceZone(),
                value.targetZone(),
                value.type(),
                value.powerRequired(),
                value.key(),
                value.animation(),
                value.sound(),
                point),
            now);
      }
      case "spawn" -> {
        ZombieSpawn value = Objects.requireNonNull(zombieSpawns.get(entityId), "spawn");
        yield withZombieSpawn(
            new ZombieSpawn(
                value.id(),
                value.name(),
                value.zone(),
                value.weight(),
                value.capacity(),
                value.minimumRound(),
                value.maximumRound(),
                value.minimumDistance(),
                value.maximumDistance(),
                value.visible(),
                value.allowedTypes(),
                value.cooldownTicks(),
                point),
            now);
      }
      case "object" -> {
        MapObject value = Objects.requireNonNull(objects.get(entityId), "object");
        yield withObject(
            new MapObject(
                value.id(), value.type(), value.name(), value.zone(), point, value.properties()),
            now);
      }
      default -> throw new IllegalArgumentException("Unknown entity kind " + kind);
    };
  }

  public MapDefinition duplicated(String kind, String entityId, String newId, Instant now) {
    if (!safeId(newId)) {
      throw new IllegalArgumentException("Invalid duplicate id");
    }
    return switch (kind) {
      case "zone" -> {
        Zone value = Objects.requireNonNull(zones.get(entityId), "zone");
        yield withZone(
            new Zone(
                newId,
                value.name() + " copy",
                value.color(),
                value.music(),
                value.ambience(),
                value.volume(),
                value.anchor()),
            now);
      }
      case "door" -> {
        Door value = Objects.requireNonNull(doors.get(entityId), "door");
        yield withDoor(
            new Door(
                newId,
                value.name() + " copy",
                value.price(),
                value.sourceZone(),
                value.targetZone(),
                value.type(),
                value.powerRequired(),
                value.key(),
                value.animation(),
                value.sound(),
                value.position()),
            now);
      }
      case "spawn" -> {
        ZombieSpawn value = Objects.requireNonNull(zombieSpawns.get(entityId), "spawn");
        yield withZombieSpawn(
            new ZombieSpawn(
                newId,
                value.name() + " copy",
                value.zone(),
                value.weight(),
                value.capacity(),
                value.minimumRound(),
                value.maximumRound(),
                value.minimumDistance(),
                value.maximumDistance(),
                value.visible(),
                value.allowedTypes(),
                value.cooldownTicks(),
                value.position()),
            now);
      }
      case "object" -> {
        MapObject value = Objects.requireNonNull(objects.get(entityId), "object");
        yield withObject(
            new MapObject(
                newId,
                value.type(),
                value.name() + " copy",
                value.zone(),
                value.position(),
                value.properties()),
            now);
      }
      default -> throw new IllegalArgumentException("Unknown entity kind " + kind);
    };
  }

  private MapDefinition copy(
      Instant now,
      Optional<MapPoint> spawn,
      Map<String, Zone> newZones,
      Map<String, Door> newDoors,
      Map<String, ZombieSpawn> newSpawns,
      Map<String, MapObject> newObjects) {
    return new MapDefinition(
        schemaVersion,
        id,
        displayName,
        description,
        creator,
        createdAt,
        now,
        world,
        icon,
        image,
        maximumPlayers,
        music,
        difficulty,
        gameMode,
        spawn,
        newZones,
        newDoors,
        newSpawns,
        newObjects);
  }

  private static String required(String value, int maximumLength, String field) {
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return limited(value, maximumLength, field);
  }

  private static String limited(String value, int maximumLength, String field) {
    String normalized = value.strip();
    if (normalized.length() > maximumLength) {
      throw new IllegalArgumentException(field + " is too long");
    }
    return normalized;
  }

  public static boolean safeId(String value) {
    return value != null && value.matches("[a-z0-9][a-z0-9_-]{0,63}");
  }

  public record Zone(
      String id,
      String name,
      String color,
      String music,
      String ambience,
      double volume,
      MapPoint anchor) {
    public Zone {
      if (!safeId(id) || name.isBlank() || volume < 0 || volume > 2) {
        throw new IllegalArgumentException("Invalid zone");
      }
      Objects.requireNonNull(color, "color");
      Objects.requireNonNull(music, "music");
      Objects.requireNonNull(ambience, "ambience");
      Objects.requireNonNull(anchor, "anchor");
    }
  }

  public record Door(
      String id,
      String name,
      int price,
      String sourceZone,
      String targetZone,
      String type,
      boolean powerRequired,
      String key,
      String animation,
      String sound,
      MapPoint position) {
    public Door {
      if (!safeId(id) || name.isBlank() || price < 0) {
        throw new IllegalArgumentException("Invalid door");
      }
      Objects.requireNonNull(sourceZone, "sourceZone");
      Objects.requireNonNull(targetZone, "targetZone");
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(animation, "animation");
      Objects.requireNonNull(sound, "sound");
      Objects.requireNonNull(position, "position");
    }
  }

  public record ZombieSpawn(
      String id,
      String name,
      String zone,
      double weight,
      int capacity,
      int minimumRound,
      int maximumRound,
      double minimumDistance,
      double maximumDistance,
      boolean visible,
      Set<String> allowedTypes,
      int cooldownTicks,
      MapPoint position) {
    public ZombieSpawn {
      if (!safeId(id)
          || name.isBlank()
          || weight <= 0
          || capacity <= 0
          || minimumRound < 1
          || maximumRound < minimumRound
          || minimumDistance < 0
          || maximumDistance < minimumDistance
          || cooldownTicks < 0) {
        throw new IllegalArgumentException("Invalid zombie spawn");
      }
      Objects.requireNonNull(zone, "zone");
      allowedTypes = Set.copyOf(allowedTypes);
      Objects.requireNonNull(position, "position");
    }
  }

  public record MapObject(
      String id,
      MapObjectType type,
      String name,
      String zone,
      MapPoint position,
      Map<String, String> properties) {
    public MapObject {
      if (!safeId(id) || name.isBlank()) {
        throw new IllegalArgumentException("Invalid map object");
      }
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(zone, "zone");
      Objects.requireNonNull(position, "position");
      properties = Map.copyOf(properties);
    }
  }
}
