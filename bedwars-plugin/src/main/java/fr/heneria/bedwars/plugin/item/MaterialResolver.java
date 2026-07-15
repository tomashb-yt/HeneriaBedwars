package fr.heneria.bedwars.plugin.item;

import java.util.Optional;
import org.bukkit.Material;

/** Case-insensitive modern Bukkit material resolver that excludes all air variants. */
public final class MaterialResolver {
  /** Resolves case-insensitively and rejects unknown and air materials without throwing. */
  public Optional<Material> resolve(String value) {
    if (value == null) return Optional.empty();
    Material material = Material.matchMaterial(value);
    if (material == null
        || material == Material.AIR
        || material == Material.CAVE_AIR
        || material == Material.VOID_AIR) return Optional.empty();
    return Optional.of(material);
  }
}
