package fr.heneria.bedwars.core.map.operation;

import fr.heneria.bedwars.core.map.MapId;
import java.time.Instant;
import java.util.UUID;

public record MapOperationSnapshot(
    MapId mapId,
    MapOperationType type,
    Instant startedAt,
    UUID initiatedBy,
    MapOperationStatus status,
    String detail) {}
