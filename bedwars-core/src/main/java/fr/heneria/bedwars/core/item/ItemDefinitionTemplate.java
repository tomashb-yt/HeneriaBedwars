package fr.heneria.bedwars.core.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable partial definition used while resolving inheritance. */
public final class ItemDefinitionTemplate {
  private final ItemKey key;
  private final ItemKey parent;
  private final String material;
  private final Integer amount;
  private final ItemText name;
  private final List<ItemText> lore;
  private final Boolean glow;
  private final Boolean unbreakable;
  private final Integer customModelData;
  private final boolean customModelDataSet;
  private final Set<String> flags;
  private final Map<String, Integer> enchantments;
  private final Integer leatherColor;
  private final HeadDefinition head;
  private final Map<String, String> tags;
  private final Set<String> requiredPlaceholders;

  private ItemDefinitionTemplate(Builder builder) {
    key = builder.key;
    parent = builder.parent;
    material = builder.material;
    amount = builder.amount;
    name = builder.name;
    lore = builder.lore == null ? null : List.copyOf(builder.lore);
    glow = builder.glow;
    unbreakable = builder.unbreakable;
    customModelData = builder.customModelData;
    customModelDataSet = builder.customModelDataSet;
    flags = builder.flags == null ? null : Set.copyOf(builder.flags);
    enchantments = builder.enchantments == null ? null : Map.copyOf(builder.enchantments);
    leatherColor = builder.leatherColor;
    head = builder.head;
    tags = builder.tags == null ? null : Map.copyOf(builder.tags);
    requiredPlaceholders =
        builder.requiredPlaceholders == null ? null : Set.copyOf(builder.requiredPlaceholders);
  }

  public static Builder builder(ItemKey key) {
    return new Builder(key);
  }

  public ItemKey key() {
    return key;
  }

  public ItemKey parent() {
    return parent;
  }

  public ItemDefinitionTemplate inherit(ItemDefinitionTemplate base) {
    Builder merged = builder(key);
    merged.material = material != null ? material : base.material;
    merged.amount = amount != null ? amount : base.amount;
    merged.name = name != null ? name : base.name;
    merged.lore = lore != null ? lore : base.lore;
    merged.glow = glow != null ? glow : base.glow;
    merged.unbreakable = unbreakable != null ? unbreakable : base.unbreakable;
    merged.customModelDataSet = customModelDataSet || base.customModelDataSet;
    merged.customModelData = customModelDataSet ? customModelData : base.customModelData;
    merged.flags = mergeSet(base.flags, flags);
    merged.enchantments = mergeMap(base.enchantments, enchantments);
    merged.leatherColor = leatherColor != null ? leatherColor : base.leatherColor;
    merged.head = head != null ? head : base.head;
    merged.tags = mergeMap(base.tags, tags);
    merged.requiredPlaceholders = mergeSet(base.requiredPlaceholders, requiredPlaceholders);
    return merged.build();
  }

  public ItemDefinition resolve(String fallbackMaterial) {
    return new ItemDefinition(
        key,
        material == null ? fallbackMaterial : material,
        amount == null ? 1 : amount,
        name == null ? ItemText.direct("") : name,
        lore == null ? List.of() : lore,
        Boolean.TRUE.equals(glow),
        Boolean.TRUE.equals(unbreakable),
        customModelData,
        flags == null ? Set.of() : flags,
        enchantments == null ? Map.of() : enchantments,
        leatherColor,
        head,
        tags == null ? Map.of() : tags,
        requiredPlaceholders == null ? Set.of() : requiredPlaceholders);
  }

  private static <T> Set<T> mergeSet(Set<T> parent, Set<T> child) {
    if (parent == null && child == null) return null;
    Set<T> result = new LinkedHashSet<>();
    if (parent != null) result.addAll(parent);
    if (child != null) result.addAll(child);
    return result;
  }

  private static <K, V> Map<K, V> mergeMap(Map<K, V> parent, Map<K, V> child) {
    if (parent == null && child == null) return null;
    Map<K, V> result = new LinkedHashMap<>();
    if (parent != null) result.putAll(parent);
    if (child != null) result.putAll(child);
    return result;
  }

  public static final class Builder {
    private final ItemKey key;
    private ItemKey parent;
    private String material;
    private Integer amount;
    private ItemText name;
    private List<ItemText> lore;
    private Boolean glow;
    private Boolean unbreakable;
    private Integer customModelData;
    private boolean customModelDataSet;
    private Set<String> flags;
    private Map<String, Integer> enchantments;
    private Integer leatherColor;
    private HeadDefinition head;
    private Map<String, String> tags;
    private Set<String> requiredPlaceholders;

    private Builder(ItemKey key) {
      this.key = key;
    }

    public Builder parent(ItemKey value) {
      parent = value;
      return this;
    }

    public Builder material(String value) {
      material = value;
      return this;
    }

    public Builder amount(Integer value) {
      amount = value;
      return this;
    }

    public Builder name(ItemText value) {
      name = value;
      return this;
    }

    public Builder lore(List<ItemText> value) {
      lore = value == null ? null : new ArrayList<>(value);
      return this;
    }

    public Builder glow(Boolean value) {
      glow = value;
      return this;
    }

    public Builder unbreakable(Boolean value) {
      unbreakable = value;
      return this;
    }

    public Builder customModelData(Integer value) {
      customModelData = value;
      customModelDataSet = true;
      return this;
    }

    public Builder itemFlags(Set<String> value) {
      flags = value;
      return this;
    }

    public Builder enchantments(Map<String, Integer> value) {
      enchantments = value;
      return this;
    }

    public Builder leatherColor(Integer value) {
      leatherColor = value;
      return this;
    }

    public Builder head(HeadDefinition value) {
      head = value;
      return this;
    }

    public Builder tags(Map<String, String> value) {
      tags = value;
      return this;
    }

    public Builder requiredPlaceholders(Set<String> value) {
      requiredPlaceholders = value;
      return this;
    }

    public ItemDefinitionTemplate build() {
      return new ItemDefinitionTemplate(this);
    }
  }
}
