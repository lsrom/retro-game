package com.github.retro_game.retro_game.model.behavior;

import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.entity.User;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The code-only behavior of a unit — the part of a unit that has no
 * representation in the content catalog and so cannot be made data-driven yet:
 * its propulsion (which drive it flies on, its base speed and its fuel
 * consumption) and its rapid fire.
 *
 * <h2>Propulsion</h2>
 *
 * <p>A unit's propulsion is modelled as an ordered list of {@link Propulsion}
 * tiers. A tier becomes <em>available</em> once the user reaches {@code minLevel}
 * in its {@code drive} technology; the unit always flies on the highest-indexed
 * available tier. The first tier is the unit's baseline and has {@code minLevel}
 * 0, so it is always available.
 *
 * <p>This captures both shapes the legacy {@code UnitItem} subclasses had:
 * <ul>
 *   <li>A fixed drive — a single tier, e.g. the Large Cargo always flies on
 *       Combustion Drive at base speed 7500 with consumption 50.</li>
 *   <li>A drive that upgrades at a technology threshold — two tiers, e.g. the
 *       Small Cargo flies on Combustion Drive (speed 5000, consumption 10)
 *       until Impulse Drive reaches level 5, then on Impulse Drive (speed
 *       10000, consumption 20).</li>
 * </ul>
 *
 * <p>A unit with no propulsion at all (every defensive structure, and the solar
 * satellite) has an empty tier list: no drive, zero base speed, zero
 * consumption — exactly the {@code UnitItem} base-class defaults.
 *
 * <p>An instance is shared per unit kind and looked up through
 * {@link ItemBehaviorRegistry}.
 */
public final class UnitBehavior {
  /**
   * One propulsion tier: a unit flies on technology {@code drive} once the user
   * reaches {@code minLevel} in it, at the given base speed and deuterium
   * consumption.
   *
   * @param drive       the drive technology this tier flies on
   * @param minLevel    the level of {@code drive} at which this tier unlocks; 0 for the baseline tier
   * @param baseSpeed   the unit's base speed on this tier
   * @param consumption the unit's deuterium consumption on this tier
   */
  public record Propulsion(TechnologyKind drive, int minLevel, int baseSpeed, int consumption) {
  }

  /** A unit with no propulsion and no rapid fire — the neutral default. */
  public static final UnitBehavior NONE = new UnitBehavior(List.of(), Map.of());

  // Ordered from the baseline tier upwards; the active tier is the last one the
  // user qualifies for. Empty for a unit that does not fly.
  private final List<Propulsion> propulsionTiers;
  private final Map<UnitKind, Integer> rapidFireAgainst;

  public UnitBehavior(List<Propulsion> propulsionTiers, Map<UnitKind, Integer> rapidFireAgainst) {
    this.propulsionTiers = List.copyOf(propulsionTiers);
    this.rapidFireAgainst = Map.copyOf(rapidFireAgainst);
  }

  // The highest-indexed tier the user qualifies for, or null if the unit does
  // not fly. Earlier tiers (lower index) are fallbacks for lower tech levels.
  @Nullable
  private Propulsion activePropulsion(User user) {
    Propulsion active = null;
    for (var tier : propulsionTiers) {
      if (user.getTechnologyLevel(tier.drive()) >= tier.minLevel()) {
        active = tier;
      }
    }
    return active;
  }

  /** The drive technology the unit currently flies on for the given user; null if it has none. */
  @Nullable
  public TechnologyKind getDrive(User user) {
    var active = activePropulsion(user);
    return active != null ? active.drive() : null;
  }

  /** The unit's base speed for the given user's drive technology levels; 0 if it does not fly. */
  public int getBaseSpeed(User user) {
    var active = activePropulsion(user);
    return active != null ? active.baseSpeed() : 0;
  }

  /** The unit's deuterium consumption for the given user's drive technology levels; 0 if it does not fly. */
  public int getConsumption(User user) {
    var active = activePropulsion(user);
    return active != null ? active.consumption() : 0;
  }

  /** How many extra shots this unit gets against each other unit kind. */
  public Map<UnitKind, Integer> getRapidFireAgainst() {
    return rapidFireAgainst;
  }
}
