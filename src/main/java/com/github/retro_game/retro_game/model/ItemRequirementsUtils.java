package com.github.retro_game.retro_game.model;

import com.github.retro_game.retro_game.entity.Body;
import com.github.retro_game.retro_game.entity.BuildingKind;
import com.github.retro_game.retro_game.entity.ItemType;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.User;
import com.github.retro_game.retro_game.service.CatalogService;

import java.util.Map;

/**
 * A helper to check whether the build/research requirements for a given item
 * are met.
 *
 * <p>An item's prerequisites are read from the content catalog
 * ({@link CatalogService#getRequirements(String)}) rather than the hardcoded
 * {@code model/} classes, so requirement edits made through the admin panel
 * take effect on the running game. The methods are keyed by an item's kind
 * String (e.g. {@code "SHIPYARD"}); callers pass {@code kind.name()}.
 */
public class ItemRequirementsUtils {
  /** Whether the given building levels satisfy the item's building prerequisites. */
  public static boolean meetsBuildingsRequirements(String kind, Map<BuildingKind, Integer> buildings) {
    return requirements(kind).stream()
        .filter(req -> req.requiredType() == ItemType.BUILDING)
        .allMatch(req -> {
          var buildingKind = buildingKindOrNull(req.requiredKind());
          // A requirement on a kind that is not a built-in building cannot be
          // satisfied from the level map; treat it as unmet, matching the
          // legacy behavior where unknown buildings had level 0.
          return buildingKind != null && buildings.getOrDefault(buildingKind, 0) >= req.requiredLevel();
        });
  }

  /** Whether the body's building levels satisfy the item's building prerequisites. */
  public static boolean meetsBuildingsRequirements(String kind, Body body) {
    return requirements(kind).stream()
        .filter(req -> req.requiredType() == ItemType.BUILDING)
        .allMatch(req -> {
          var buildingKind = buildingKindOrNull(req.requiredKind());
          return buildingKind != null && body.getBuildingLevel(buildingKind) >= req.requiredLevel();
        });
  }

  /** Whether the given technology levels satisfy the item's technology prerequisites. */
  public static boolean meetsTechnologiesRequirements(String kind, Map<TechnologyKind, Integer> technologies) {
    return requirements(kind).stream()
        .filter(req -> req.requiredType() == ItemType.TECHNOLOGY)
        .allMatch(req -> {
          var technologyKind = technologyKindOrNull(req.requiredKind());
          return technologyKind != null && technologies.getOrDefault(technologyKind, 0) >= req.requiredLevel();
        });
  }

  /** Whether the user's technology levels satisfy the item's technology prerequisites. */
  public static boolean meetsTechnologiesRequirements(String kind, User user) {
    return requirements(kind).stream()
        .filter(req -> req.requiredType() == ItemType.TECHNOLOGY)
        .allMatch(req -> {
          var technologyKind = technologyKindOrNull(req.requiredKind());
          return technologyKind != null && user.getTechnologyLevel(technologyKind) >= req.requiredLevel();
        });
  }

  /** Whether the body and its owner satisfy all of the item's prerequisites. */
  public static boolean meetsRequirements(String kind, Body body) {
    return meetsBuildingsRequirements(kind, body) && meetsTechnologiesRequirements(kind, body.getUser());
  }

  private static java.util.List<CatalogService.Requirement> requirements(String kind) {
    return CatalogService.getInstance().getRequirements(kind);
  }

  private static BuildingKind buildingKindOrNull(String kind) {
    try {
      return BuildingKind.valueOf(kind);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static TechnologyKind technologyKindOrNull(String kind) {
    try {
      return TechnologyKind.valueOf(kind);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
