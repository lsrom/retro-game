package com.github.retro_game.retro_game.entity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts between the two representations of per-item player state (building
 * levels, unit counts, technology levels).
 *
 * <ul>
 *   <li><b>Stored form</b> — a {@code Map<String, Integer>} keyed by the stable
 *       item name (e.g. {@code "METAL_MINE"}), persisted as a {@code jsonb}
 *       column. Keying by name rather than by array position means adding,
 *       removing or reordering items no longer shifts every stored value.</li>
 *   <li><b>API form</b> — a kind-keyed {@link EnumMap} that the rest of the
 *       game works with.</li>
 * </ul>
 *
 * <p>An item absent from the stored map counts as 0, so items introduced later
 * need no backfill of existing rows.
 */
final class ItemMaps {
  private ItemMaps() {
  }

  /** Reads one item's value from a stored map; an absent item counts as 0. */
  static int get(Map<String, Integer> stored, Enum<?> kind) {
    var value = stored.getOrDefault(kind.name(), 0);
    assert value >= 0;
    return value;
  }

  /** Expands a stored map into a full kind-keyed map, with 0 for every absent item. */
  static <K extends Enum<K>> EnumMap<K, Integer> toEnumMap(Class<K> kindClass, Map<String, Integer> stored) {
    var result = new EnumMap<K, Integer>(kindClass);
    for (var kind : kindClass.getEnumConstants()) {
      result.put(kind, stored.getOrDefault(kind.name(), 0));
    }
    return result;
  }

  /** Converts a kind-keyed map into the name-keyed form used for storage. */
  static <K extends Enum<K>> Map<String, Integer> toStored(Map<K, Integer> items) {
    var stored = new HashMap<String, Integer>();
    for (var entry : items.entrySet()) {
      var value = entry.getValue();
      assert value >= 0;
      stored.put(entry.getKey().name(), value);
    }
    return stored;
  }
}
