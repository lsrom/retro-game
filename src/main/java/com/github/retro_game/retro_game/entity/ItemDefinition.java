package com.github.retro_game.retro_game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A single content-catalog entry — one building, technology, or unit.
 *
 * <p>This is the data-driven replacement for the hardcoded item classes under
 * {@code model/}. In phase 1 the rows are seeded from those classes and edited
 * through the admin panel; the game itself does not yet read from here.
 *
 * <p>Some fields only apply to certain {@link ItemType}s: {@code baseEnergy} to
 * buildings; {@code unitType}, {@code capacity}, {@code weapons}, {@code shield}
 * and {@code armor} to units. {@code costFactor} applies to buildings and
 * technologies — their cost grows by {@code costFactor^(level-1)} — while units
 * have a flat cost.
 */
@Entity
@Table(name = "item_definitions")
@Getter
@Setter
public class ItemDefinition {
  @Column(name = "id")
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ItemType type;

  // Stable identifier, e.g. "METAL_MINE"; matches the legacy enum constant for seeded items.
  @Column(name = "kind", nullable = false, updatable = false)
  private String kind;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "metal_cost", nullable = false)
  private double metalCost;

  @Column(name = "crystal_cost", nullable = false)
  private double crystalCost;

  @Column(name = "deuterium_cost", nullable = false)
  private double deuteriumCost;

  @Column(name = "cost_factor", nullable = false)
  private double costFactor;

  @Column(name = "base_energy", nullable = false)
  private int baseEnergy;

  @Column(name = "unit_type")
  @Enumerated(EnumType.STRING)
  private UnitType unitType;

  @Column(name = "capacity", nullable = false)
  private long capacity;

  @Column(name = "weapons", nullable = false)
  private double weapons;

  @Column(name = "shield", nullable = false)
  private double shield;

  @Column(name = "armor", nullable = false)
  private double armor;
}
