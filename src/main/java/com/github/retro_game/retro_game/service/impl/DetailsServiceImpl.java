package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.dto.*;
import com.github.retro_game.retro_game.entity.*;
import com.github.retro_game.retro_game.model.CatalogItem;
import com.github.retro_game.retro_game.model.ItemCostUtils;
import com.github.retro_game.retro_game.model.ItemTimeUtils;
import com.github.retro_game.retro_game.repository.UserRepository;
import com.github.retro_game.retro_game.security.CustomUser;
import com.github.retro_game.retro_game.service.CatalogService;
import com.github.retro_game.retro_game.service.DetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class DetailsServiceImpl implements DetailsService {
  private final int buildingQueueCapacity;
  private final ItemTimeUtils itemTimeUtils;
  private final UserRepository userRepository;
  private BodyServiceInternal bodyServiceInternal;
  private BuildingsServiceInternal buildingsServiceInternal;
  private UnitService unitService;

  public DetailsServiceImpl(@Value("${retro-game.building-queue-capacity}") int buildingQueueCapacity,
                            ItemTimeUtils itemTimeUtils, UserRepository userRepository) {
    this.buildingQueueCapacity = buildingQueueCapacity;
    this.itemTimeUtils = itemTimeUtils;
    this.userRepository = userRepository;
  }

  @Autowired
  public void setBodyServiceInternal(BodyServiceInternal bodyServiceInternal) {
    this.bodyServiceInternal = bodyServiceInternal;
  }

  @Autowired
  public void setBuildingsServiceInternal(BuildingsServiceInternal buildingsServiceInternal) {
    this.buildingsServiceInternal = buildingsServiceInternal;
  }

  @Autowired
  public void setUnitService(UnitService unitService) {
    this.unitService = unitService;
  }

  @PostConstruct
  private void checkProperties() {
    Assert.isTrue(buildingQueueCapacity >= 1,
        "retro-game.building-queue-capacity must be at least 1");
  }

  @Override
  public BuildingDetailsDto getBuildingDetails(long bodyId, String kind) {
    Body body = bodyServiceInternal.getUpdated(bodyId);

    BuildingKind k;
    try {
      k = BuildingKind.valueOf(kind);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown building kind: " + kind, e);
    }

    Collection<BuildingQueueEntry> queue = body.getBuildingQueue().values();

    var currentLevel = body.getBuildingLevel(k);
    var futureLevel = currentLevel;
    for (BuildingQueueEntry entry : queue) {
      if (entry.kind() == k) {
        assert entry.action() == BuildingQueueAction.CONSTRUCT || entry.action() == BuildingQueueAction.DESTROY;
        futureLevel += entry.action() == BuildingQueueAction.CONSTRUCT ? 1 : -1;
      }
    }

    ResourcesDto destructionCost = null;
    long destructionTime = 0;

    boolean destroyable = k != BuildingKind.TERRAFORMER && k != BuildingKind.LUNAR_BASE && futureLevel >= 1;
    boolean canDestroyNow = false;

    if (destroyable) {
      var cost = ItemCostUtils.getCost(k, futureLevel - 1);
      destructionCost = Converter.convert(cost);

      var roboticsFactoryLevel = body.getBuildingLevel(BuildingKind.ROBOTICS_FACTORY);
      var naniteFactoryLevel = body.getBuildingLevel(BuildingKind.NANITE_FACTORY);
      destructionTime = itemTimeUtils.getBuildingDestructionTime(cost, roboticsFactoryLevel, naniteFactoryLevel);

      if (queue.size() < buildingQueueCapacity && (!queue.isEmpty() || body.getResources().greaterOrEqual(cost))) {
        canDestroyNow = true;
      }
    }

    return new BuildingDetailsDto(currentLevel, futureLevel, destructionCost, destructionTime, destroyable,
        canDestroyNow);
  }

  @Override
  public TechnologyDetailsDto getTechnologyDetails(long bodyId, String kind) {
    long userId = CustomUser.getCurrentUserId();
    User user = userRepository.getOne(userId);

    TechnologyKind k;
    try {
      k = TechnologyKind.valueOf(kind);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown technology kind: " + kind, e);
    }

    int currentLevel = user.getTechnologyLevel(k);
    int futureLevel = currentLevel + (int) user.getTechnologyQueue().values().stream()
        .filter(e -> e.kind() == k)
        .count();

    return new TechnologyDetailsDto(currentLevel, futureLevel);
  }

  @Override
  public UnitDetailsDto getUnitDetails(long bodyId, String kind) {
    long userId = CustomUser.getCurrentUserId();
    User user = userRepository.getOne(userId);

    UnitKind k;
    try {
      k = UnitKind.valueOf(kind);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown unit kind: " + kind, e);
    }
    CatalogItem item = CatalogItem.of(k.name());

    double weapons = unitService.getWeapons(k, user);
    double shield = unitService.getShield(k, user);
    double armor = unitService.getArmor(k, user);

    Map<UnitKindDto, Integer> rapidFireAgainst = item.getRapidFireAgainst().entrySet().stream()
        .collect(Collectors.toMap(entry -> Converter.convert(entry.getKey()), Map.Entry::getValue,
            (l, r) -> {
              throw new IllegalStateException();
            }, () -> new EnumMap<>(UnitKindDto.class)));

    // Which units have rapid fire against this one: scan every catalog unit's
    // rapid-fire behavior. (Rapid fire is keyed by the built-in UnitKind set.)
    Map<UnitKindDto, Integer> rapidFireFrom = new EnumMap<>(UnitKindDto.class);
    for (var unit : CatalogItem.allOfType(ItemType.UNIT)) {
      var n = unit.getRapidFireAgainst().get(k);
      if (n != null) {
        rapidFireFrom.put(Converter.convert(UnitKind.valueOf(unit.getKind())), n);
      }
    }

    // Base weapons, shield and armor are read from the editable content catalog.
    var definition = CatalogService.getInstance().getDefinition(k.name());
    return new UnitDetailsDto(weapons, shield, armor, (int) item.getCapacity(), item.getConsumption(user),
        unitService.getSpeed(k, user), definition.getWeapons(), definition.getShield(), definition.getArmor(),
        item.getBaseSpeed(user), rapidFireAgainst, rapidFireFrom);
  }
}
