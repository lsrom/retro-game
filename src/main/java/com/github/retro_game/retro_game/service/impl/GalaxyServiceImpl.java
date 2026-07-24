package com.github.retro_game.retro_game.service.impl;

import com.github.retro_game.retro_game.cache.AllianceTagCache;
import com.github.retro_game.retro_game.cache.StatisticsCache;
import com.github.retro_game.retro_game.cache.UserAllianceCache;
import com.github.retro_game.retro_game.dto.ActiveStateDto;
import com.github.retro_game.retro_game.dto.GalaxySlotDto;
import com.github.retro_game.retro_game.dto.NoobProtectionRankDto;
import com.github.retro_game.retro_game.dto.StatisticsSummaryDto;
import com.github.retro_game.retro_game.entity.CombatResult;
import com.github.retro_game.retro_game.entity.CoordinatesKind;
import com.github.retro_game.retro_game.entity.DebrisField;
import com.github.retro_game.retro_game.entity.GalaxySlot;
import com.github.retro_game.retro_game.battleengine.UnitKind;
import com.github.retro_game.retro_game.entity.User;
import com.github.retro_game.retro_game.repository.CombatReportRepository;
import com.github.retro_game.retro_game.repository.DebrisFieldRepository;
import com.github.retro_game.retro_game.repository.GalaxySlotRepository;
import com.github.retro_game.retro_game.repository.SimplifiedCombatReportRepository;
import com.github.retro_game.retro_game.repository.UserRepository;
import com.github.retro_game.retro_game.security.CustomUser;
import com.github.retro_game.retro_game.service.ActivityService;
import com.github.retro_game.retro_game.service.GalaxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class GalaxyServiceImpl implements GalaxyService {
  private static final Logger logger = LoggerFactory.getLogger(GalaxyServiceImpl.class);
  private final GalaxySlotRepository galaxySlotRepository;
  private final CombatReportRepository combatReportRepository;
  private final DebrisFieldRepository debrisFieldRepository;
  private final SimplifiedCombatReportRepository simplifiedCombatReportRepository;
  private final AllianceTagCache allianceTagCache;
  private final StatisticsCache statisticsCache;
  private final UserAllianceCache userAllianceCache;
  private final UserRepository userRepository;
  private ActivityService activityService;
  private NoobProtectionService noobProtectionService;
  private UnitService unitService;
  private UserServiceInternal userServiceInternal;

  public GalaxyServiceImpl(GalaxySlotRepository galaxySlotRepository, AllianceTagCache allianceTagCache,
                           StatisticsCache statisticsCache, UserAllianceCache userAllianceCache,
                           UserRepository userRepository, CombatReportRepository combatReportRepository,
                           DebrisFieldRepository debrisFieldRepository,
                           SimplifiedCombatReportRepository simplifiedCombatReportRepository) {
    this.galaxySlotRepository = galaxySlotRepository;
    this.combatReportRepository = combatReportRepository;
    this.debrisFieldRepository = debrisFieldRepository;
    this.simplifiedCombatReportRepository = simplifiedCombatReportRepository;
    this.allianceTagCache = allianceTagCache;
    this.statisticsCache = statisticsCache;
    this.userAllianceCache = userAllianceCache;
    this.userRepository = userRepository;
  }

  @Autowired
  public void setActivityService(ActivityService activityService) {
    this.activityService = activityService;
  }

  @Autowired
  public void setNoobProtectionService(NoobProtectionService noobProtectionService) {
    this.noobProtectionService = noobProtectionService;
  }

  @Autowired
  public void setUnitService(UnitService unitService) {
    this.unitService = unitService;
  }

  @Autowired
  public void setUserServiceInternal(UserServiceInternal userServiceInternal) {
    this.userServiceInternal = userServiceInternal;
  }

  @Override
  public Map<Integer, GalaxySlotDto> getSlots(int galaxy, int system) {
    long userId = CustomUser.getCurrentUserId();
    var user = userRepository.getOne(userId);
    logger.info("Viewing galaxy: userId={} galaxy={} system={}", userId, galaxy, system);

    long now = Instant.now().getEpochSecond();

    List<GalaxySlot> slots = galaxySlotRepository.findAllByGalaxyAndSystem(galaxy, system);
    var attackAgainPositions = getAttackAgainPositions(user, galaxy, system);

    // Get the activities of bodies.
    List<Long> ids = new ArrayList<>();
    for (GalaxySlot slot : slots) {
      ids.add(slot.getPlanetId());
      if (slot.getMoonId() != null) {
        ids.add(slot.getMoonId());
      }
    }
    Map<Long, Long> activities = activityService.getBodiesActivities(ids);

    Map<Integer, GalaxySlotDto> ret = new HashMap<>();
    for (GalaxySlot slot : slots) {
      boolean onVacation = slot.getVacationUntil() != null;
      boolean banned = userServiceInternal.isBanned(slot.getVacationUntil(), slot.isForcedVacation());
      NoobProtectionRankDto noobProtectionRank = noobProtectionService.getOtherPlayerRank(userId, slot.getUserId());

      boolean shortInactive = false;
      boolean longInactive = false;
      ActiveStateDto activeState = activityService.activeState(slot.getUserId());
      switch (activeState) {
        case INACTIVE_LONG:
          longInactive = true;
        case INACTIVE_SHORT:
          shortInactive = true;
          break;
        default:
          break;
      }

      StatisticsSummaryDto summary = statisticsCache.getUserSummary(slot.getUserId());
      int rank = summary == null ? 0 : summary.overall().rank();

      long activityAt = activities.getOrDefault(slot.getPlanetId(), 0L);
      if (slot.getMoonId() != null) {
        activityAt = Math.max(activityAt, activities.getOrDefault(slot.getMoonId(), 0L));
      }
      int activity = (int) ((now - activityAt) / 60L);
      if (activity < 15) {
        activity = 0;
      } else if (activity >= 60) {
        activity = 60;
      }

      var debrisMetal = slot.getDebrisMetal() != null ? slot.getDebrisMetal() : 0L;
      var debrisCrystal = slot.getDebrisCrystal() != null ? slot.getDebrisCrystal() : 0L;
      var recyclerCapacity = unitService.getCapacity(UnitKind.RECYCLER, user);
      var neededRecyclers = (int) Math.ceil((double) (debrisMetal + debrisCrystal) / recyclerCapacity);

      Long allianceId = userAllianceCache.getUserAlliance(slot.getUserId());
      String allianceTag = allianceId == null ? null : allianceTagCache.getTag(allianceId);

      boolean own = slot.getUserId() == userId;
      boolean attackAgainAvailable = attackAgainPositions.contains(slot.getPosition());

      GalaxySlotDto s = new GalaxySlotDto(slot.getUserId(), slot.getUserName(), rank, onVacation, banned,
          noobProtectionRank, slot.getPlanetName(), Converter.convert(slot.getPlanetType()), slot.getPlanetImage(),
          slot.getMoonName(), slot.getMoonImage(), activity, debrisMetal, debrisCrystal, neededRecyclers, allianceId,
          allianceTag, own, shortInactive, longInactive, attackAgainAvailable);
      ret.put(slot.getPosition(), s);
    }
    debrisFieldRepository.findByKey_GalaxyAndKey_SystemAndKey_Position(galaxy, system, 16)
        .ifPresent(debrisField -> ret.put(16, makeUnknownSpaceSlot(user, debrisField)));
    return ret;
  }

  private GalaxySlotDto makeUnknownSpaceSlot(User user, DebrisField debrisField) {
    var debrisMetal = debrisField.getMetal();
    var debrisCrystal = debrisField.getCrystal();
    var recyclerCapacity = unitService.getCapacity(UnitKind.RECYCLER, user);
    var neededRecyclers = (int) Math.ceil((double) (debrisMetal + debrisCrystal) / recyclerCapacity);
    return new GalaxySlotDto(0L, null, 0, false, false, NoobProtectionRankDto.EQUAL,
        null, null, 0, null, null, 60, debrisMetal, debrisCrystal, neededRecyclers,
        null, null, false, false, false, false);
  }

  private HashSet<Integer> getAttackAgainPositions(User user, int galaxy, int system) {
    var reports = simplifiedCombatReportRepository
        .findByUserAndDeletedIsFalseAndResultAndCoordinates_GalaxyAndCoordinates_SystemAndCoordinates_KindAndCombatReportIdIsNotNullOrderByAtDesc(
            user, CombatResult.WIN, galaxy, system, CoordinatesKind.PLANET);
    var combatReportIds = reports.stream()
        .map(report -> report.getCombatReportId())
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    var combatReports = combatReportRepository.findAllById(combatReportIds).stream()
        .collect(Collectors.toMap(report -> report.getId(), Function.identity()));
    var positions = new HashSet<Integer>();
    for (var report : reports) {
      var combatReport = combatReports.get(report.getCombatReportId());
      if (combatReport != null && Arrays.stream(combatReport.getAttackers()).anyMatch(id -> id == user.getId())) {
        positions.add(report.getCoordinates().getPosition());
      }
    }
    return positions;
  }

  @Override
  public Map<Integer, GalaxySlotDto> getSlots(long bodyId, int galaxy, int system) {
    return getSlots(galaxy, system);
  }
}
