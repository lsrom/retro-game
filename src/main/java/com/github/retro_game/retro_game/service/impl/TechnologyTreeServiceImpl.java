package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.dto.*;
import com.github.retro_game.retro_game.entity.BuildingKind;
import com.github.retro_game.retro_game.entity.ItemType;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.entity.UnitType;
import com.github.retro_game.retro_game.model.CatalogItem;
import com.github.retro_game.retro_game.service.TechnologyTreeService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

@Service
class TechnologyTreeServiceImpl implements TechnologyTreeService {
  // Built lazily on first request, not in a static initializer: the technology
  // tree's requirements are read from the content catalog, which is not yet
  // loaded when this class is initialized. volatile + double-checked locking
  // keeps the one-time build safe under concurrent first requests.
  private static volatile TechnologyTreeDto technologyTree;

  @Override
  public TechnologyTreeDto getTechnologyTree(long bodyId) {
    var tree = technologyTree;
    if (tree == null) {
      synchronized (TechnologyTreeServiceImpl.class) {
        tree = technologyTree;
        if (tree == null) {
          tree = buildTechnologyTree();
          technologyTree = tree;
        }
      }
    }
    return tree;
  }

  private static TechnologyTreeDto buildTechnologyTree() {
    // Iterate the content catalog rather than the hardcoded item classes, so
    // requirement edits made through the admin panel are reflected. An item
    // whose kind is not a built-in *Kind (a future admin-created item) is
    // skipped, as the DTO maps are keyed by the *KindDto enums.
    Map<BuildingKindDto, RequirementsDto> buildings = new EnumMap<>(BuildingKindDto.class);
    for (var item : CatalogItem.allOfType(ItemType.BUILDING)) {
      BuildingKind kind;
      try {
        kind = BuildingKind.valueOf(item.getKind());
      } catch (IllegalArgumentException e) {
        continue;
      }
      buildings.put(Converter.convert(kind), requirementsOf(item));
    }

    Map<TechnologyKindDto, RequirementsDto> technologies = new EnumMap<>(TechnologyKindDto.class);
    for (var item : CatalogItem.allOfType(ItemType.TECHNOLOGY)) {
      TechnologyKind kind;
      try {
        kind = TechnologyKind.valueOf(item.getKind());
      } catch (IllegalArgumentException e) {
        continue;
      }
      technologies.put(Converter.convert(kind), requirementsOf(item));
    }

    Map<UnitKindDto, RequirementsDto> fleet = new EnumMap<>(UnitKindDto.class);
    Map<UnitKindDto, RequirementsDto> defense = new EnumMap<>(UnitKindDto.class);
    for (var item : CatalogItem.allOfType(ItemType.UNIT)) {
      UnitKind kind;
      try {
        kind = UnitKind.valueOf(item.getKind());
      } catch (IllegalArgumentException e) {
        continue;
      }
      var target = item.getUnitType() == UnitType.DEFENSE ? defense : fleet;
      target.put(Converter.convert(kind), requirementsOf(item));
    }

    return new TechnologyTreeDto(
        Collections.unmodifiableMap(buildings),
        Collections.unmodifiableMap(technologies),
        Collections.unmodifiableMap(fleet),
        Collections.unmodifiableMap(defense));
  }

  private static RequirementsDto requirementsOf(CatalogItem item) {
    return new RequirementsDto(
        Converter.convertBuildingsRequirements(item.getBuildingsRequirements()),
        Converter.convertTechnologiesRequirements(item.getTechnologiesRequirements()));
  }
}
