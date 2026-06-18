package com.github.retro_game.retro_game.model;

import com.github.retro_game.retro_game.entity.Body;
import com.github.retro_game.retro_game.entity.BuildingKind;
import com.github.retro_game.retro_game.entity.ItemDefinition;
import com.github.retro_game.retro_game.entity.ItemType;
import com.github.retro_game.retro_game.entity.Resources;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.entity.UnitType;
import com.github.retro_game.retro_game.entity.User;
import com.github.retro_game.retro_game.model.behavior.ItemBehaviorRegistry;
import com.github.retro_game.retro_game.service.CatalogService;
import org.springframework.lang.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A unified, kind-keyed view of one catalog item — the seam the data-driven
 * content rebuild codes against, in place of the {@code *Kind} enums and the
 * (now removed) hardcoded {@code BuildingItem}/{@code TechnologyItem}/{@code
 * UnitItem} classes.
 *
 * <p>It joins the two halves of an item's definition:
 * <ul>
 *   <li><b>Data</b> — cost, cost factor, required energy, cargo capacity,
 *       combat stats and build requirements — read from the content catalog
 *       ({@link CatalogService}), so it reflects admin-panel edits.</li>
 *   <li><b>Behavior</b> — special build requirements, propulsion, fuel
 *       consumption and rapid fire — which is still expressed in code, with no
 *       catalog representation. It is keyed by the item's {@code kind} String
 *       and supplied by {@link ItemBehaviorRegistry}: a built-in item gets the
 *       behavior moved out of its old model class, while an admin-created item
 *       (whose kind is in no {@code *Kind} enum) gets neutral defaults — no
 *       special requirement, no drive, zero consumption and speed, no rapid
 *       fire. Making that behavior data-driven too is future work.</li>
 * </ul>
 *
 * <p>Instances are cheap, immutable wrappers; build them with the factory
 * methods rather than caching them, so catalog edits are always picked up.
 */
public class CatalogItem {
  private final ItemDefinition definition;

  private CatalogItem(ItemDefinition definition) {
    this.definition = definition;
  }

  /** Wraps a catalog definition. */
  public static CatalogItem of(ItemDefinition definition) {
    return new CatalogItem(definition);
  }

  /** Returns the catalog item with the given kind, e.g. {@code "METAL_MINE"}. */
  public static CatalogItem of(String kind) {
    return of(CatalogService.getInstance().getDefinition(kind));
  }

  /** Returns every catalog item of the given type, ordered by id (seed order). */
  public static List<CatalogItem> allOfType(ItemType type) {
    return CatalogService.getInstance().getAllByType(type).stream().map(CatalogItem::of).toList();
  }

  /**
   * Returns the built-in {@link UnitKind}s of the given {@link UnitType} (fleet
   * ships or defensive structures), read from the content catalog.
   *
   * <p>This replaces the old {@code UnitItem.getFleet()} / {@code
   * UnitItem.getDefense()} key sets. A catalog unit whose kind is not a
   * built-in {@code UnitKind} — a future admin-created unit — is skipped, just
   * as it had no entry in those maps.
   */
  public static Set<UnitKind> unitKindsOfType(UnitType unitType) {
    var kinds = EnumSet.noneOf(UnitKind.class);
    for (var item : allOfType(ItemType.UNIT)) {
      if (item.getUnitType() != unitType) {
        continue;
      }
      try {
        kinds.add(UnitKind.valueOf(item.getKind()));
      } catch (IllegalArgumentException e) {
        // Not a built-in unit kind; skip it.
      }
    }
    return kinds;
  }

  // --- Identity and data, from the content catalog ---

  public String getKind() {
    return definition.getKind();
  }

  public ItemType getType() {
    return definition.getType();
  }

  public String getName() {
    return definition.getName();
  }

  /** The level-1 cost; the cost at higher levels grows by {@link #getCostFactor()}. */
  public Resources getBaseCost() {
    return new Resources(definition.getMetalCost(), definition.getCrystalCost(), definition.getDeuteriumCost());
  }

  /** The per-level cost multiplier. Always 1 for units, which have a flat cost. */
  public double getCostFactor() {
    return definition.getCostFactor();
  }

  /** The energy a building requires at level 1; 0 for technologies and units. */
  public int getBaseEnergy() {
    return definition.getBaseEnergy();
  }

  /** Whether a unit belongs to the fleet or the defense; null for non-units. */
  @Nullable
  public UnitType getUnitType() {
    return definition.getUnitType();
  }

  public long getCapacity() {
    return definition.getCapacity();
  }

  public double getWeapons() {
    return definition.getWeapons();
  }

  public double getShield() {
    return definition.getShield();
  }

  public double getArmor() {
    return definition.getArmor();
  }

  /**
   * The buildings, with their levels, required before this item can be built,
   * read from the content catalog. A required kind that is not a built-in
   * {@link BuildingKind} (a future admin-created building) is skipped.
   */
  public Map<BuildingKind, Integer> getBuildingsRequirements() {
    var requirements = new EnumMap<BuildingKind, Integer>(BuildingKind.class);
    for (var req : CatalogService.getInstance().getRequirements(getKind())) {
      if (req.requiredType() != ItemType.BUILDING) {
        continue;
      }
      try {
        requirements.put(BuildingKind.valueOf(req.requiredKind()), req.requiredLevel());
      } catch (IllegalArgumentException e) {
        // Not a built-in building kind; skip it.
      }
    }
    return requirements;
  }

  /**
   * The technologies, with their levels, required before this item can be
   * built, read from the content catalog. A required kind that is not a
   * built-in {@link TechnologyKind} (a future admin-created technology) is
   * skipped.
   */
  public Map<TechnologyKind, Integer> getTechnologiesRequirements() {
    var requirements = new EnumMap<TechnologyKind, Integer>(TechnologyKind.class);
    for (var req : CatalogService.getInstance().getRequirements(getKind())) {
      if (req.requiredType() != ItemType.TECHNOLOGY) {
        continue;
      }
      try {
        requirements.put(TechnologyKind.valueOf(req.requiredKind()), req.requiredLevel());
      } catch (IllegalArgumentException e) {
        // Not a built-in technology kind; skip it.
      }
    }
    return requirements;
  }

  // --- Behavior, still expressed in code (supplied by ItemBehaviorRegistry) ---

  /** Whether a building may be constructed on the given body (e.g. moon-only buildings). */
  public boolean meetsSpecialRequirements(Body body) {
    // Only buildings have a body restriction; technologies and units have none.
    return getType() != ItemType.BUILDING
        || ItemBehaviorRegistry.buildingBehavior(getKind()).meetsSpecialRequirements(body);
  }

  /** A unit's deuterium consumption for the given user's drive technology levels. */
  public int getConsumption(User user) {
    return getType() == ItemType.UNIT ? ItemBehaviorRegistry.unitBehavior(getKind()).getConsumption(user) : 0;
  }

  /** The drive technology a unit currently flies on for the given user; null if it has none. */
  @Nullable
  public TechnologyKind getDrive(User user) {
    return getType() == ItemType.UNIT ? ItemBehaviorRegistry.unitBehavior(getKind()).getDrive(user) : null;
  }

  /** A unit's base speed for the given user's drive technology levels. */
  public int getBaseSpeed(User user) {
    return getType() == ItemType.UNIT ? ItemBehaviorRegistry.unitBehavior(getKind()).getBaseSpeed(user) : 0;
  }

  /** How many extra shots this unit gets against each other unit kind. */
  public Map<UnitKind, Integer> getRapidFireAgainst() {
    return getType() == ItemType.UNIT ? ItemBehaviorRegistry.unitBehavior(getKind()).getRapidFireAgainst() : Map.of();
  }
}
