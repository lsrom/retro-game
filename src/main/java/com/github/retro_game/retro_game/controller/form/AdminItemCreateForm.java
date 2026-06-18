package com.github.retro_game.retro_game.controller.form;

import com.github.retro_game.retro_game.entity.ItemType;
import com.github.retro_game.retro_game.entity.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * The fields of a brand-new catalog item, bound from the admin create-item form.
 *
 * <p>Unlike {@link AdminItemForm} (which edits an existing item) this form also
 * carries the item's {@link ItemType} and its stable {@code kind} — both fixed
 * once the item exists. As with editing, every value field is present
 * regardless of type; {@code AdminItemService} applies only the ones relevant
 * to the chosen type.
 */
@Getter
@Setter
public class AdminItemCreateForm {
  @NotNull
  private ItemType type;

  // The stable identifier, e.g. "METAL_MINE". Kept to the legacy enum-constant
  // convention (upper case, digits and underscores) so it can later key player
  // state and the catalog interchangeably.
  @NotBlank
  @Pattern(regexp = "[A-Z][A-Z0-9_]*")
  private String kind;

  @NotBlank
  private String name;

  @PositiveOrZero
  private double metalCost;

  @PositiveOrZero
  private double crystalCost;

  @PositiveOrZero
  private double deuteriumCost;

  // Applies to buildings and technologies; must be >= 1 for those (validated in
  // the service). Ignored for units, which have a flat cost.
  @PositiveOrZero
  private double costFactor = 2.0;

  // Applies to buildings.
  @PositiveOrZero
  private int baseEnergy;

  // Applies to units: FLEET or DEFENSE. Required when type is UNIT.
  private UnitType unitType;

  // The following apply to units.
  @PositiveOrZero
  private long capacity;

  @PositiveOrZero
  private double weapons;

  @PositiveOrZero
  private double shield;

  @PositiveOrZero
  private double armor;
}
