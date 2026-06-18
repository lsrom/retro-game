package com.github.retro_game.retro_game.controller.form;

import com.github.retro_game.retro_game.entity.ItemDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * The editable fields of a catalog item, bound from the admin item-edit form.
 *
 * <p>Every field is present regardless of item type; {@code AdminItemService}
 * only applies the ones relevant to the item being edited, so the type's
 * unused fields cannot be clobbered.
 */
@Getter
@Setter
public class AdminItemForm {
  private long id;

  @NotBlank
  private String name;

  @PositiveOrZero
  private double metalCost;

  @PositiveOrZero
  private double crystalCost;

  @PositiveOrZero
  private double deuteriumCost;

  // Applies to buildings and technologies; the page enforces >= 1 for those.
  @PositiveOrZero
  private double costFactor;

  // Applies to buildings.
  @PositiveOrZero
  private int baseEnergy;

  // The following apply to units.
  @PositiveOrZero
  private long capacity;

  @PositiveOrZero
  private double weapons;

  @PositiveOrZero
  private double shield;

  @PositiveOrZero
  private double armor;

  /** Builds a form pre-populated from an existing catalog item. */
  public static AdminItemForm fromItem(ItemDefinition item) {
    var form = new AdminItemForm();
    form.id = item.getId();
    form.name = item.getName();
    form.metalCost = item.getMetalCost();
    form.crystalCost = item.getCrystalCost();
    form.deuteriumCost = item.getDeuteriumCost();
    form.costFactor = item.getCostFactor();
    form.baseEnergy = item.getBaseEnergy();
    form.capacity = item.getCapacity();
    form.weapons = item.getWeapons();
    form.shield = item.getShield();
    form.armor = item.getArmor();
    return form;
  }
}
