package com.github.retro_game.retro_game.model.behavior;

import com.github.retro_game.retro_game.entity.BuildingKind;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.retro_game.retro_game.model.behavior.UnitBehavior.Propulsion;

/**
 * The single place that supplies the code-only <em>behavior</em> of a catalog
 * item, keyed by the item's {@code kind} String.
 *
 * <p>Most of an item is data — cost, combat stats, build requirements — and
 * lives in the content catalog ({@code item_definitions} /
 * {@code item_requirements}), so it can be edited through the admin panel. A
 * little is not: a building's special build requirement and a unit's
 * propulsion and rapid fire. That residue used to live in the hardcoded
 * {@code BuildingItem} / {@code UnitItem} subclasses; it now lives here, moved
 * across verbatim, one entry per built-in kind.
 *
 * <h2>Built-in vs. admin-created kinds</h2>
 *
 * <p>The 57 built-in items have an entry here, registered under the same kind
 * String as their {@code BuildingKind} / {@code TechnologyKind} / {@code
 * UnitKind} enum constant. An item created through the admin panel has a kind
 * that is not in any of those enums, and no entry here; the lookups fall back
 * to neutral defaults — {@link BuildingBehavior#ANY_BODY} and
 * {@link UnitBehavior#NONE} — which is exactly the behavior {@link
 * com.github.retro_game.retro_game.model.CatalogItem} documents for such items
 * (no special requirement, no drive, zero consumption and speed, no rapid
 * fire). Technologies have no code-only behavior at all.
 *
 * <p>This is a stateless lookup table built once at class load; it touches
 * neither the database nor the catalog, so it is safe to use from any thread
 * and at any point in start-up.
 */
public final class ItemBehaviorRegistry {
  private static final Map<String, BuildingBehavior> BUILDING_BEHAVIORS = buildBuildingBehaviors();
  private static final Map<String, UnitBehavior> UNIT_BEHAVIORS = buildUnitBehaviors();

  private ItemBehaviorRegistry() {
  }

  /**
   * The behavior of the building with the given kind. An unknown kind — a
   * building created through the admin panel — has no special requirement
   * ({@link BuildingBehavior#ANY_BODY}).
   */
  public static BuildingBehavior buildingBehavior(String kind) {
    return BUILDING_BEHAVIORS.getOrDefault(kind, BuildingBehavior.ANY_BODY);
  }

  /**
   * The behavior of the unit with the given kind. An unknown kind — a unit
   * created through the admin panel — has no propulsion and no rapid fire
   * ({@link UnitBehavior#NONE}).
   */
  public static UnitBehavior unitBehavior(String kind) {
    return UNIT_BEHAVIORS.getOrDefault(kind, UnitBehavior.NONE);
  }

  // --- Built-in building behavior -------------------------------------------

  private static Map<String, BuildingBehavior> buildBuildingBehaviors() {
    var m = new EnumMap<BuildingKind, BuildingBehavior>(BuildingKind.class);
    // Resource buildings and other planet structures can be built on planets only.
    m.put(BuildingKind.METAL_MINE, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.CRYSTAL_MINE, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.DEUTERIUM_SYNTHESIZER, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.SOLAR_PLANT, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.FUSION_REACTOR, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.METAL_STORAGE, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.CRYSTAL_STORAGE, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.DEUTERIUM_TANK, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.RESEARCH_LAB, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.TERRAFORMER, BuildingBehavior.PLANET_ONLY);
    m.put(BuildingKind.MISSILE_SILO, BuildingBehavior.PLANET_ONLY);
    // Moon structures can be built on moons only.
    m.put(BuildingKind.LUNAR_BASE, BuildingBehavior.MOON_ONLY);
    m.put(BuildingKind.SENSOR_PHALANX, BuildingBehavior.MOON_ONLY);
    m.put(BuildingKind.JUMP_GATE, BuildingBehavior.MOON_ONLY);
    // Robotics factory, nanite factory, shipyard and alliance depot have no
    // body restriction; they fall through to the ANY_BODY default and so need
    // no explicit entry. (NANITE_FACTORY's moon handling depends on a config
    // flag and stays in BuildingsServiceImpl, as before.)
    var result = new HashMap<String, BuildingBehavior>();
    m.forEach((kind, behavior) -> result.put(kind.name(), behavior));
    return Map.copyOf(result);
  }

  // --- Built-in unit behavior -----------------------------------------------

  private static Map<String, UnitBehavior> buildUnitBehaviors() {
    var m = new EnumMap<UnitKind, UnitBehavior>(UnitKind.class);

    // Fleet ships. Propulsion tiers are ordered baseline-first; a unit upgrades
    // to a later tier once it reaches that tier's drive-technology level.
    m.put(UnitKind.SMALL_CARGO, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.COMBUSTION_DRIVE, 0, 5000, 10),
            new Propulsion(TechnologyKind.IMPULSE_DRIVE, 5, 10000, 20)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5)));

    m.put(UnitKind.LARGE_CARGO, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.COMBUSTION_DRIVE, 0, 7500, 50)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5)));

    m.put(UnitKind.LITTLE_FIGHTER, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.COMBUSTION_DRIVE, 0, 12500, 20)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5)));

    m.put(UnitKind.HEAVY_FIGHTER, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.IMPULSE_DRIVE, 0, 10000, 75)),
        rapidFire(UnitKind.SMALL_CARGO, 3, UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5)));

    m.put(UnitKind.CRUISER, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.IMPULSE_DRIVE, 0, 15000, 300)),
        rapidFire(UnitKind.LITTLE_FIGHTER, 6, UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5,
            UnitKind.ROCKET_LAUNCHER, 10)));

    m.put(UnitKind.BATTLESHIP, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.HYPERSPACE_DRIVE, 0, 10000, 500)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5)));

    m.put(UnitKind.COLONY_SHIP, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.IMPULSE_DRIVE, 0, 2500, 1000)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5)));

    m.put(UnitKind.RECYCLER, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.COMBUSTION_DRIVE, 0, 2000, 300)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5)));

    m.put(UnitKind.ESPIONAGE_PROBE, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.COMBUSTION_DRIVE, 0, 100000000, 1)),
        Map.of()));

    // The bomber upgrades from Impulse Drive to Hyperspace Drive at level 8;
    // its consumption is the same (1000) on both tiers.
    m.put(UnitKind.BOMBER, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.IMPULSE_DRIVE, 0, 4000, 1000),
            new Propulsion(TechnologyKind.HYPERSPACE_DRIVE, 8, 5000, 1000)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5, UnitKind.ROCKET_LAUNCHER, 20,
            UnitKind.LIGHT_LASER, 20, UnitKind.HEAVY_LASER, 10, UnitKind.ION_CANNON, 10)));

    // The solar satellite does not fly: no propulsion, no rapid fire.
    m.put(UnitKind.SOLAR_SATELLITE, UnitBehavior.NONE);

    m.put(UnitKind.DESTROYER, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.HYPERSPACE_DRIVE, 0, 5000, 1000)),
        rapidFire(UnitKind.ESPIONAGE_PROBE, 5, UnitKind.SOLAR_SATELLITE, 5, UnitKind.LIGHT_LASER, 10)));

    m.put(UnitKind.DEATH_STAR, new UnitBehavior(
        List.of(new Propulsion(TechnologyKind.HYPERSPACE_DRIVE, 0, 100, 1)),
        deathStarRapidFire()));

    // Defensive structures (rocket launcher through interplanetary missile) do
    // not fly and have no rapid fire; they fall through to UnitBehavior.NONE
    // and so need no explicit entry.

    var result = new HashMap<String, UnitBehavior>();
    m.forEach((kind, behavior) -> result.put(kind.name(), behavior));
    return Map.copyOf(result);
  }

  // Builds a rapid-fire map from flattened (kind, count) pairs.
  private static Map<UnitKind, Integer> rapidFire(Object... kindCountPairs) {
    assert kindCountPairs.length % 2 == 0 : "rapid fire pairs must come in (kind, count) twos";
    var m = new EnumMap<UnitKind, Integer>(UnitKind.class);
    for (var i = 0; i < kindCountPairs.length; i += 2) {
      m.put((UnitKind) kindCountPairs[i], (Integer) kindCountPairs[i + 1]);
    }
    return m;
  }

  private static Map<UnitKind, Integer> deathStarRapidFire() {
    var m = new EnumMap<UnitKind, Integer>(UnitKind.class);
    m.put(UnitKind.SMALL_CARGO, 250);
    m.put(UnitKind.LARGE_CARGO, 250);
    m.put(UnitKind.LITTLE_FIGHTER, 200);
    m.put(UnitKind.HEAVY_FIGHTER, 100);
    m.put(UnitKind.CRUISER, 33);
    m.put(UnitKind.BATTLESHIP, 30);
    m.put(UnitKind.COLONY_SHIP, 250);
    m.put(UnitKind.RECYCLER, 250);
    m.put(UnitKind.ESPIONAGE_PROBE, 1250);
    m.put(UnitKind.BOMBER, 25);
    m.put(UnitKind.SOLAR_SATELLITE, 1250);
    m.put(UnitKind.DESTROYER, 5);
    m.put(UnitKind.ROCKET_LAUNCHER, 200);
    m.put(UnitKind.LIGHT_LASER, 200);
    m.put(UnitKind.HEAVY_LASER, 100);
    m.put(UnitKind.GAIUS_CANNON, 50);
    m.put(UnitKind.ION_CANNON, 100);
    return m;
  }
}
