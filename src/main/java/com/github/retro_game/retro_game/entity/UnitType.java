package com.github.retro_game.retro_game.entity;

/**
 * The sub-category of a catalog unit: a fleet ship or a defensive structure.
 * Only set on item definitions whose {@link ItemType} is {@code UNIT}.
 */
public enum UnitType {
  FLEET,
  DEFENSE
}
