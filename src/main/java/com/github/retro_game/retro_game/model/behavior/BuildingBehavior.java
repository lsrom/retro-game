package com.github.retro_game.retro_game.model.behavior;

import com.github.retro_game.retro_game.entity.Body;
import com.github.retro_game.retro_game.entity.CoordinatesKind;

/**
 * The code-only behavior of a building — the part of a building that has no
 * representation in the content catalog and so cannot be made data-driven yet.
 *
 * <p>For now this is just the <em>special requirement</em>: a predicate on the
 * body a building is being constructed on. Most buildings can be built anywhere,
 * but the resource buildings are planet-only and the moon buildings are
 * moon-only.
 *
 * <p>An instance is shared per building kind and looked up through
 * {@link ItemBehaviorRegistry}; see that class for how an unknown
 * (admin-created) kind is handled.
 */
public enum BuildingBehavior {
  /** May be built on any body — the default for buildings with no restriction. */
  ANY_BODY {
    @Override
    public boolean meetsSpecialRequirements(Body body) {
      return true;
    }
  },

  /** May be built only on a planet, e.g. the resource mines and storages. */
  PLANET_ONLY {
    @Override
    public boolean meetsSpecialRequirements(Body body) {
      return body.getCoordinates().getKind() == CoordinatesKind.PLANET;
    }
  },

  /** May be built only on a moon, e.g. the lunar base and jump gate. */
  MOON_ONLY {
    @Override
    public boolean meetsSpecialRequirements(Body body) {
      return body.getCoordinates().getKind() == CoordinatesKind.MOON;
    }
  };

  /** Whether this building may be constructed on the given body. */
  public abstract boolean meetsSpecialRequirements(Body body);
}
