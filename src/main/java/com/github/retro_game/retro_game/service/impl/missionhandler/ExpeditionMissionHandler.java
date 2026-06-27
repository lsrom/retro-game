package com.github.retro_game.retro_game.service.impl.missionhandler;

import com.github.retro_game.retro_game.battleengine.BattleEngine;
import com.github.retro_game.retro_game.battleengine.BattleOutcome;
import com.github.retro_game.retro_game.battleengine.Combatant;
import com.github.retro_game.retro_game.battleengine.CombatantOutcome;
import com.github.retro_game.retro_game.battleengine.UnitGroupStats;
import com.github.retro_game.retro_game.dto.MoonCreationResultDto;
import com.github.retro_game.retro_game.entity.BattleResult;
import com.github.retro_game.retro_game.entity.Body;
import com.github.retro_game.retro_game.entity.Event;
import com.github.retro_game.retro_game.entity.EventKind;
import com.github.retro_game.retro_game.entity.ExpeditionEventType;
import com.github.retro_game.retro_game.entity.Flight;
import com.github.retro_game.retro_game.entity.Resources;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.model.ItemCostUtils;
import com.github.retro_game.retro_game.repository.FlightRepository;
import com.github.retro_game.retro_game.service.ActivityService;
import com.github.retro_game.retro_game.service.impl.BodyServiceInternal;
import com.github.retro_game.retro_game.service.impl.CombatReportServiceInternal;
import com.github.retro_game.retro_game.service.impl.EventScheduler;
import com.github.retro_game.retro_game.service.impl.ReportServiceInternal;
import com.github.retro_game.retro_game.service.impl.UnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExpeditionMissionHandler {
  private static final long PIRATE_USER_ID = -1L;
  private static final long ALIEN_USER_ID = -2L;
  private static final String PIRATE_USER_NAME = "Pirates";
  private static final String ALIEN_USER_NAME = "Aliens";
  private static final Logger logger = LoggerFactory.getLogger(ExpeditionMissionHandler.class);
  private static final String NOTHING_REPORT_TEXT = "Expedition reports nothing of interest happened during the " +
      "mission. Fleet returned safely without any results.";
  private static final String ONE_HOUR_DELAY_REPORT_TEXT = "Expedition captain ran into and old friend on a space " +
      "station and they were drinking the whole night. Because of this, expedition will return later.";
  private static final String TWO_HOURS_DELAY_REPORT_TEXT = "Expedition encountered malfunction on capital ship " +
      "engines which caused delay.";
  private static final String THREE_HOURS_DELAY_REPORT_TEXT = "Terrible epidemic of moribundus spread across the " +
      "entire expedition fleet, causing massive delay.";
  private static final String FLEET_LOSS_REPORT_TEXT = "Last thing we heard from expedition fleet was something " +
      "about taking suspicious alien egg on board. After that, only some screams. The fleet is lost.";
  private static final String PIRATES_REPORT_TEXT = "Fleet has encountered pirates. Unlike the ones with straw hat " +
      "flag, they were not friendly.";
  private static final String ALIENS_REPORT_TEXT = "Fleet has encountered aliens.";
  private static final String ORE_ASTEROID_REPORT_TEXT = "Expedition fleet found a giant asteroid rich with minerals " +
      "and they were able to extract them. The fleet is bringing back %d metal and %d crystal.";
  private static final String GAS_CLOUD_REPORT_TEXT = "Expedition fleet detected extreme amounts of gas molecules " +
      "and was able to extract %d deuterium.";
  private static final String SPECTACULAR_SUPERNOVA_REPORT_TEXT = "Fleet brought back stunning pictures of exploding " +
      "supernova. It was all they could talk about and they didn't manage to get anything else done.";
  private static final String WARP_WINDOW_REPORT_TEXT = "Expedition found warp singularity in interstellar space and " +
      "used it to get back sooner.";
  private static final String RESCUE_SHIPS_REPORT_TEXT = "Expedition fleet discovered abandoned ships floating in " +
      "space. Fleet engineers were able to repair some of them and they were added to the fleet. Found ships: %s.";
  private static final String RESCUE_FLEET_REPORT_TEXT = "Long range sensors revealed dead fleet floating in space. " +
      "Upon careful inspection, some ships were repaired and brought back. Found ships: %s.";

  private final ActivityService activityService;
  private final BattleEngine battleEngine;
  private final BodyServiceInternal bodyServiceInternal;
  private final CombatReportServiceInternal combatReportServiceInternal;
  private final EventScheduler eventScheduler;
  private final FlightRepository flightRepository;
  private final MissionHandlerUtils missionHandlerUtils;
  private final ReportServiceInternal reportServiceInternal;
  private final UnitService unitService;

  public ExpeditionMissionHandler(ActivityService activityService, BattleEngine battleEngine,
                                  BodyServiceInternal bodyServiceInternal,
                                  CombatReportServiceInternal combatReportServiceInternal, EventScheduler eventScheduler,
                                  FlightRepository flightRepository, MissionHandlerUtils missionHandlerUtils,
                                  ReportServiceInternal reportServiceInternal, UnitService unitService) {
    this.activityService = activityService;
    this.battleEngine = battleEngine;
    this.bodyServiceInternal = bodyServiceInternal;
    this.combatReportServiceInternal = combatReportServiceInternal;
    this.eventScheduler = eventScheduler;
    this.flightRepository = flightRepository;
    this.missionHandlerUtils = missionHandlerUtils;
    this.reportServiceInternal = reportServiceInternal;
    this.unitService = unitService;
  }

  public void handle(Flight flight, Date at) {
    long eventAt = at.toInstant().getEpochSecond();
    long arrivalAt = flight.getArrivalAt().toInstant().getEpochSecond();
    long holdUntil = flight.getHoldUntil().toInstant().getEpochSecond();

    if (eventAt == arrivalAt && arrivalAt != holdUntil) {
      logger.info("Expedition started: flightId={} startUserId={} startBodyId={} targetCoordinates={}" +
              " arrivalAt='{}' holdUntil='{}'",
          flight.getId(), flight.getStartUser().getId(), flight.getStartBody().getId(), flight.getTargetCoordinates(),
          flight.getArrivalAt(), flight.getHoldUntil());
      Event event = new Event();
      event.setAt(flight.getHoldUntil());
      event.setKind(EventKind.FLIGHT);
      event.setParam(flight.getId());
      eventScheduler.schedule(event);
      return;
    }

    ExpeditionEventType eventType = pickExpeditionEventType();
    logger.info("Expedition event processed: flightId={} startUserId={} startBodyId={} targetCoordinates={}" +
            " holdUntil='{}' eventType={}",
        flight.getId(), flight.getStartUser().getId(), flight.getStartBody().getId(), flight.getTargetCoordinates(),
        flight.getHoldUntil(), eventType);
    if (eventType == ExpeditionEventType.Nothing) {
      reportServiceInternal.createExpeditionReport(flight, NOTHING_REPORT_TEXT);
    } else if (eventType == ExpeditionEventType.Delay) {
      handleDelay(flight);
    } else if (eventType == ExpeditionEventType.FleetLoss) {
      handleFleetLoss(flight);
      return;
    } else if (eventType == ExpeditionEventType.Pirates) {
      if (!handlePirates(flight)) {
        return;
      }
    } else if (eventType == ExpeditionEventType.Aliens) {
      if (!handleAliens(flight)) {
        return;
      }
    } else if (eventType == ExpeditionEventType.OreAsteroid) {
      handleOreAsteroid(flight);
    } else if (eventType == ExpeditionEventType.GasCloud) {
      handleGasCloud(flight);
    } else if (eventType == ExpeditionEventType.SpectacularSupernova) {
      reportServiceInternal.createExpeditionReport(flight, SPECTACULAR_SUPERNOVA_REPORT_TEXT);
    } else if (eventType == ExpeditionEventType.WarpWindow) {
      handleWarpWindow(flight);
      return;
    } else if (eventType == ExpeditionEventType.RescueShips) {
      handleRescueShips(flight);
    } else if (eventType == ExpeditionEventType.RescueFleet) {
      handleRescueFleet(flight);
    }
    missionHandlerUtils.scheduleReturn(flight);
  }

  private void handleFleetLoss(Flight flight) {
    reportServiceInternal.createExpeditionReport(flight, FLEET_LOSS_REPORT_TEXT);
    flightRepository.delete(flight);
  }

  private boolean handlePirates(Flight flight) {
    return handleHostileEncounter(flight, PIRATES_REPORT_TEXT,
        calculateEncounterFleet(flight, PIRATE_USER_ID, PIRATE_USER_NAME, 0.3, 0.4, false));
  }

  private boolean handleAliens(Flight flight) {
    return handleHostileEncounter(flight, ALIENS_REPORT_TEXT,
        calculateEncounterFleet(flight, ALIEN_USER_ID, ALIEN_USER_NAME, 0.6, 0.3, true));
  }

  private void handleRescueShips(Flight flight) {
    var rescuedFleet = calculateEncounterFleet(flight, 0L, "Rescued ships", 0.05, 0.0, false);
    addRescuedShips(flight, rescuedFleet.combatant().unitGroups());
    reportServiceInternal.createExpeditionReport(flight,
        String.format(RESCUE_SHIPS_REPORT_TEXT, formatFoundShips(rescuedFleet.combatant().unitGroups())));
  }

  private void handleRescueFleet(Flight flight) {
    var rescuedFleet = calculateEncounterFleet(flight, 0L, "Rescued fleet", 0.2, 0.0, false);
    addRescuedShips(flight, rescuedFleet.combatant().unitGroups());
    reportServiceInternal.createExpeditionReport(flight,
        String.format(RESCUE_FLEET_REPORT_TEXT, formatFoundShips(rescuedFleet.combatant().unitGroups())));
  }

  private void handleOreAsteroid(Flight flight) {
    long maxResources = (long) Math.floor(getRemainingCargoSpace(flight) * 0.2);
    long totalResources = pickResourceAmount(maxResources);
    long metal = totalResources == 0 ? 0 : ThreadLocalRandom.current().nextLong(totalResources + 1);
    long crystal = totalResources - metal;

    Resources resources = flight.getResources();
    resources.setMetal(resources.getMetal() + metal);
    resources.setCrystal(resources.getCrystal() + crystal);
    reportServiceInternal.createExpeditionReport(flight, String.format(ORE_ASTEROID_REPORT_TEXT, metal, crystal));
  }

  private void handleGasCloud(Flight flight) {
    long maxDeuterium = (long) Math.floor(getRemainingCargoSpace(flight) * 0.1);
    long deuterium = pickResourceAmount(maxDeuterium);

    Resources resources = flight.getResources();
    resources.setDeuterium(resources.getDeuterium() + deuterium);
    reportServiceInternal.createExpeditionReport(flight, String.format(GAS_CLOUD_REPORT_TEXT, deuterium));
  }

  private void handleWarpWindow(Flight flight) {
    flight.setReturnAt(flight.getHoldUntil());
    reportServiceInternal.createExpeditionReport(flight, WARP_WINDOW_REPORT_TEXT);

    Body body = flight.getStartBody();
    activityService.handleBodyActivity(body.getId(), flight.getReturnAt().toInstant().getEpochSecond());
    bodyServiceInternal.updateResourcesAndShipyard(body, flight.getReturnAt());
    body.getResources().add(flight.getResources());
    deployUnits(flight, body);
    flightRepository.delete(flight);
    reportServiceInternal.createReturnReport(flight);
  }

  private static void deployUnits(Flight flight, Body body) {
    for (var entry : flight.getUnits().entrySet()) {
      UnitKind kind = entry.getKey();
      int count = entry.getValue();
      body.setUnitsCount(kind, body.getUnitsCount(kind) + count);
    }
  }

  private long getRemainingCargoSpace(Flight flight) {
    long capacity = 0L;
    for (var entry : flight.getUnits().entrySet()) {
      capacity += entry.getValue() * unitService.getCapacity(entry.getKey(), flight.getStartUser());
    }
    return Math.max(0L, capacity - (long) Math.ceil(flight.getResources().total()));
  }

  private static long pickResourceAmount(long maxResources) {
    if (maxResources <= 0) {
      return 0;
    }
    return ThreadLocalRandom.current().nextLong(1, maxResources + 1);
  }

  private boolean handleHostileEncounter(
          Flight flight,
          String expeditionReportText,
          HostileFleet hostileFleet
  ) {
    reportServiceInternal.createExpeditionReport(flight, expeditionReportText);

    Combatant expeditionFleet = makeExpeditionCombatant(flight);
    var attackers = List.of(expeditionFleet);
    var defenders = List.of(hostileFleet.combatant());
    int seed = ThreadLocalRandom.current().nextInt();
    long startTime = System.nanoTime();
    BattleOutcome battleOutcome = battleEngine.fight(attackers, defenders, seed);
    long executionTime = System.nanoTime() - startTime;

    int lastRound = battleOutcome.numRounds() - 1;
    var attackerStats = battleOutcome.attackersOutcomes().get(0).getNthRoundUnitGroupsStats(lastRound);
    var defenderStats = battleOutcome.defendersOutcomes().get(0).getNthRoundUnitGroupsStats(lastRound);
    boolean attackersAlive = hasRemainingUnits(attackerStats);
    boolean defendersAlive = hasRemainingUnits(defenderStats);
    BattleResult battleResult;
    if (attackersAlive && defendersAlive) {
      battleResult = BattleResult.DRAW;
    } else if (attackersAlive) {
      battleResult = BattleResult.ATTACKERS_WIN;
    } else {
      battleResult = BattleResult.DEFENDERS_WIN;
    }

    Resources attackersLoss = calculateLoss(expeditionFleet, battleOutcome.attackersOutcomes().get(0), lastRound);
    Resources defendersLoss = calculateLoss(hostileFleet.combatant(), battleOutcome.defendersOutcomes().get(0),
        lastRound);
    var emptyResources = new Resources();
    var moonCreationResult = new MoonCreationResultDto(0.0, false);
    var combatReport = combatReportServiceInternal.create(flight.getHoldUntil(), attackers, defenders, battleOutcome,
        battleResult, attackersLoss, defendersLoss, emptyResources, emptyResources, moonCreationResult, null, seed,
        executionTime);
    reportServiceInternal.createSimplifiedCombatReport(flight.getStartUser(), true, flight.getHoldUntil(), (Long) null,
        hostileFleet.name(), flight.getTargetCoordinates(), battleResult, battleOutcome.numRounds(), attackersLoss,
        defendersLoss, emptyResources, emptyResources, moonCreationResult, combatReport);

    applyRemainingUnits(flight, attackerStats);
    if (flight.getTotalUnitsCount() == 0) {
      flightRepository.delete(flight);
      return false;
    }
    return true;
  }

  private HostileFleet calculateEncounterFleet(Flight flight, long userId, String userName, double baseSizeFactor,
                                               double randomSizeFactor, boolean strongerTechnology) {
    double sizeFactor = baseSizeFactor;
    if (randomSizeFactor > 0.0) {
      sizeFactor += ThreadLocalRandom.current().nextDouble(randomSizeFactor);
    }
    int weaponsTechnology = randomEncounterTechnology(
        flight.getStartUser().getTechnologyLevel(TechnologyKind.WEAPONS_TECHNOLOGY), strongerTechnology);
    int shieldingTechnology = randomEncounterTechnology(
        flight.getStartUser().getTechnologyLevel(TechnologyKind.SHIELDING_TECHNOLOGY), strongerTechnology);
    int armorTechnology = randomEncounterTechnology(
        flight.getStartUser().getTechnologyLevel(TechnologyKind.ARMOR_TECHNOLOGY), strongerTechnology);

    var units = new EnumMap<UnitKind, Long>(UnitKind.class);
    addCombatShips(units, flight.getUnits(), sizeFactor);

    long fleetUnits = flight.getUnits().values().stream().mapToLong(Integer::longValue).sum();
    if (units.isEmpty()) {
      units.put(UnitKind.LITTLE_FIGHTER, Math.max(1L, Math.round(fleetUnits * sizeFactor)));
    }

    var combatant = new Combatant(userId, flight.getTargetCoordinates(), weaponsTechnology, shieldingTechnology,
        armorTechnology, units);
    return new HostileFleet(userName, combatant);
  }

  private static void addRescuedShips(Flight flight, Map<UnitKind, Long> rescuedShips) {
    var units = flight.getUnits();
    for (var entry : rescuedShips.entrySet()) {
      UnitKind kind = entry.getKey();
      int count = Math.toIntExact(entry.getValue());
      units.put(kind, units.getOrDefault(kind, 0) + count);
    }
    flight.setUnits(units);
  }

  private static String formatFoundShips(Map<UnitKind, Long> ships) {
    StringBuilder builder = new StringBuilder();
    for (UnitKind kind : UnitKind.values()) {
      long count = ships.getOrDefault(kind, 0L);
      if (count <= 0) {
        continue;
      }
      if (!builder.isEmpty()) {
        builder.append(", ");
      }
      builder.append(count).append(' ').append(formatUnitKind(kind));
    }
    return builder.toString();
  }

  private static String formatUnitKind(UnitKind kind) {
    StringBuilder builder = new StringBuilder();
    String[] words = kind.name().toLowerCase().split("_");
    for (String word : words) {
      if (!builder.isEmpty()) {
        builder.append(' ');
      }
      builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return builder.toString();
  }

  private static void addCombatShips(EnumMap<UnitKind, Long> hostileUnits, Map<UnitKind, Integer> expeditionUnits,
                                     double sizeFactor) {
    addScaledHostileUnits(hostileUnits, expeditionUnits, UnitKind.LITTLE_FIGHTER, sizeFactor, 1);
    addScaledHostileUnits(hostileUnits, expeditionUnits, UnitKind.HEAVY_FIGHTER, sizeFactor, 1);
    addScaledHostileUnits(hostileUnits, expeditionUnits, UnitKind.CRUISER, sizeFactor, 1);
    addScaledHostileUnits(hostileUnits, expeditionUnits, UnitKind.BATTLESHIP, sizeFactor, 1);
    addScaledHostileUnits(hostileUnits, expeditionUnits, UnitKind.BATTLE_CRUISER, sizeFactor, 1);
    addScaledHostileUnits(hostileUnits, expeditionUnits, UnitKind.BOMBER, sizeFactor, 1);
    addScaledHostileUnits(hostileUnits, expeditionUnits, UnitKind.DESTROYER, sizeFactor, 1);
  }

  private static void addScaledHostileUnits(EnumMap<UnitKind, Long> hostileUnits, Map<UnitKind, Integer> expeditionUnits,
                                            UnitKind unitKind, double sizeFactor, int minimum) {
    int expeditionCount = expeditionUnits.getOrDefault(unitKind, 0);
    if (expeditionCount <= 0) {
      return;
    }
    long hostileCount = Math.max(minimum, Math.round(expeditionCount * sizeFactor));
    hostileUnits.put(unitKind, hostileCount);
  }

  private static int randomEncounterTechnology(int expeditionTechnology, boolean strongerTechnology) {
    if (strongerTechnology) {
      return expeditionTechnology + ThreadLocalRandom.current().nextInt(1, 4);
    }
    return Math.max(0, expeditionTechnology - ThreadLocalRandom.current().nextInt(4));
  }

  private static Combatant makeExpeditionCombatant(Flight flight) {
    var user = flight.getStartUser();
    return new Combatant(user.getId(), flight.getStartBody().getCoordinates(),
        user.getTechnologyLevel(TechnologyKind.WEAPONS_TECHNOLOGY),
        user.getTechnologyLevel(TechnologyKind.SHIELDING_TECHNOLOGY),
        user.getTechnologyLevel(TechnologyKind.ARMOR_TECHNOLOGY), makeUnitGroups(flight.getUnits()));
  }

  private static EnumMap<UnitKind, Long> makeUnitGroups(Map<UnitKind, Integer> units) {
    var unitGroups = new EnumMap<UnitKind, Long>(UnitKind.class);
    for (var entry : units.entrySet()) {
      if (entry.getValue() > 0) {
        unitGroups.put(entry.getKey(), entry.getValue().longValue());
      }
    }
    return unitGroups;
  }

  private static boolean hasRemainingUnits(EnumMap<UnitKind, UnitGroupStats> unitGroupsStats) {
    return unitGroupsStats.values().stream().anyMatch(stats -> stats.numRemainingUnits() > 0);
  }

  private static Resources calculateLoss(Combatant combatant, CombatantOutcome outcome, int lastRound) {
    var result = new Resources();
    var finalStats = outcome.getNthRoundUnitGroupsStats(lastRound);
    for (var entry : combatant.unitGroups().entrySet()) {
      UnitKind kind = entry.getKey();
      long initialUnits = entry.getValue();
      UnitGroupStats stats = finalStats.get(kind);
      long remainingUnits = stats != null ? stats.numRemainingUnits() : 0;
      long lostUnits = initialUnits - remainingUnits;
      if (lostUnits <= 0) {
        continue;
      }
      Resources cost = ItemCostUtils.getCost(kind);
      cost.mul(lostUnits);
      result.add(cost);
    }
    return result;
  }

  private static void applyRemainingUnits(Flight flight, EnumMap<UnitKind, UnitGroupStats> unitGroupsStats) {
    var units = new EnumMap<UnitKind, Integer>(UnitKind.class);
    for (var entry : unitGroupsStats.entrySet()) {
      long remainingUnits = entry.getValue().numRemainingUnits();
      if (remainingUnits > 0) {
        units.put(entry.getKey(), Math.toIntExact(remainingUnits));
      }
    }
    flight.setUnits(units);
  }

  private void handleDelay(Flight flight) {
    int delayHours = pickDelayHours();
    long returnAt = flight.getReturnAt().toInstant().getEpochSecond() + delayHours * 3600L;
    flight.setReturnAt(Date.from(Instant.ofEpochSecond(returnAt)));

    String reportText;
    if (delayHours == 1) {
      reportText = ONE_HOUR_DELAY_REPORT_TEXT;
    } else if (delayHours == 2) {
      reportText = TWO_HOURS_DELAY_REPORT_TEXT;
    } else {
      reportText = THREE_HOURS_DELAY_REPORT_TEXT;
    }
    reportServiceInternal.createExpeditionReport(flight, reportText);
  }

  private int pickDelayHours() {
    int selectedWeight = ThreadLocalRandom.current().nextInt(100);
    if (selectedWeight < 50) {
      return 1;
    }
    if (selectedWeight < 80) {
      return 2;
    }
    return 3;
  }

  private ExpeditionEventType pickExpeditionEventType() {
    ExpeditionEventType[] eventTypes = ExpeditionEventType.values();
    int totalWeight = 0;
    for (ExpeditionEventType eventType : eventTypes) {
      totalWeight += eventType.getWeight();
    }
    if (totalWeight <= 0) {
      throw new IllegalStateException("Expedition event weights must sum to a positive number");
    }

    int selectedWeight = ThreadLocalRandom.current().nextInt(totalWeight);
    for (ExpeditionEventType eventType : eventTypes) {
      selectedWeight -= eventType.getWeight();
      if (selectedWeight < 0) {
        return eventType;
      }
    }

    throw new IllegalStateException("Expedition event picker failed");
  }

  private record HostileFleet(String name, Combatant combatant) {
  }
}
