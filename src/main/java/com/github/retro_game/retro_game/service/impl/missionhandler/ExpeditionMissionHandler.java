package com.github.retro_game.retro_game.service.impl.missionhandler;

import com.github.retro_game.retro_game.battleengine.BattleEngine;
import com.github.retro_game.retro_game.battleengine.BattleOutcome;
import com.github.retro_game.retro_game.battleengine.Combatant;
import com.github.retro_game.retro_game.battleengine.CombatantOutcome;
import com.github.retro_game.retro_game.battleengine.UnitGroupStats;
import com.github.retro_game.retro_game.dto.MoonCreationResultDto;
import com.github.retro_game.retro_game.entity.BattleResult;
import com.github.retro_game.retro_game.entity.Event;
import com.github.retro_game.retro_game.entity.EventKind;
import com.github.retro_game.retro_game.entity.ExpeditionEventType;
import com.github.retro_game.retro_game.entity.Flight;
import com.github.retro_game.retro_game.entity.Resources;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.model.ItemCostUtils;
import com.github.retro_game.retro_game.repository.FlightRepository;
import com.github.retro_game.retro_game.service.impl.CombatReportServiceInternal;
import com.github.retro_game.retro_game.service.impl.EventScheduler;
import com.github.retro_game.retro_game.service.impl.ReportServiceInternal;
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
  private static final String PIRATE_USER_NAME = "Pirates";
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

  private final BattleEngine battleEngine;
  private final CombatReportServiceInternal combatReportServiceInternal;
  private final EventScheduler eventScheduler;
  private final FlightRepository flightRepository;
  private final MissionHandlerUtils missionHandlerUtils;
  private final ReportServiceInternal reportServiceInternal;

  public ExpeditionMissionHandler(BattleEngine battleEngine, CombatReportServiceInternal combatReportServiceInternal,
                                  EventScheduler eventScheduler, FlightRepository flightRepository,
                                  MissionHandlerUtils missionHandlerUtils, ReportServiceInternal reportServiceInternal) {
    this.battleEngine = battleEngine;
    this.combatReportServiceInternal = combatReportServiceInternal;
    this.eventScheduler = eventScheduler;
    this.flightRepository = flightRepository;
    this.missionHandlerUtils = missionHandlerUtils;
    this.reportServiceInternal = reportServiceInternal;
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
    }
    missionHandlerUtils.scheduleReturn(flight);
  }

  private void handleFleetLoss(Flight flight) {
    reportServiceInternal.createExpeditionReport(flight, FLEET_LOSS_REPORT_TEXT);
    flightRepository.delete(flight);
  }

  private boolean handlePirates(Flight flight) {
    reportServiceInternal.createExpeditionReport(flight, PIRATES_REPORT_TEXT);

    Combatant expeditionFleet = makeExpeditionCombatant(flight);
    PirateFleet pirateFleet = calculatePirateFleet(flight);
    var attackers = List.of(expeditionFleet);
    var defenders = List.of(pirateFleet.combatant());

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
    Resources defendersLoss = calculateLoss(pirateFleet.combatant(), battleOutcome.defendersOutcomes().get(0),
        lastRound);
    var emptyResources = new Resources();
    var moonCreationResult = new MoonCreationResultDto(0.0, false);
    var combatReport = combatReportServiceInternal.create(flight.getHoldUntil(), attackers, defenders, battleOutcome,
        battleResult, attackersLoss, defendersLoss, emptyResources, emptyResources, moonCreationResult, null, seed,
        executionTime);
    reportServiceInternal.createSimplifiedCombatReport(flight.getStartUser(), true, flight.getHoldUntil(), (Long) null,
        PIRATE_USER_NAME, flight.getTargetCoordinates(), battleResult, battleOutcome.numRounds(), attackersLoss,
        defendersLoss, emptyResources, emptyResources, moonCreationResult, combatReport);

    applyRemainingUnits(flight, attackerStats);
    if (flight.getTotalUnitsCount() == 0) {
      flightRepository.delete(flight);
      return false;
    }
    return true;
  }

  private PirateFleet calculatePirateFleet(Flight flight) {
    double sizeFactor = 0.3 + ThreadLocalRandom.current().nextDouble(0.4);
    int weaponsTechnology = randomPirateTechnology(
        flight.getStartUser().getTechnologyLevel(TechnologyKind.WEAPONS_TECHNOLOGY));
    int shieldingTechnology = randomPirateTechnology(
        flight.getStartUser().getTechnologyLevel(TechnologyKind.SHIELDING_TECHNOLOGY));
    int armorTechnology = randomPirateTechnology(
        flight.getStartUser().getTechnologyLevel(TechnologyKind.ARMOR_TECHNOLOGY));

    var units = new EnumMap<UnitKind, Long>(UnitKind.class);
    addScaledPirateUnits(units, flight.getUnits(), UnitKind.LITTLE_FIGHTER, sizeFactor, 1);
    addScaledPirateUnits(units, flight.getUnits(), UnitKind.HEAVY_FIGHTER, sizeFactor, 1);
    addScaledPirateUnits(units, flight.getUnits(), UnitKind.CRUISER, sizeFactor, 1);
    addScaledPirateUnits(units, flight.getUnits(), UnitKind.BATTLESHIP, sizeFactor, 1);
    addScaledPirateUnits(units, flight.getUnits(), UnitKind.BATTLE_CRUISER, sizeFactor, 1);
    addScaledPirateUnits(units, flight.getUnits(), UnitKind.BOMBER, sizeFactor, 1);
    addScaledPirateUnits(units, flight.getUnits(), UnitKind.DESTROYER, sizeFactor, 1);

    long fleetUnits = flight.getUnits().values().stream().mapToLong(Integer::longValue).sum();
    if (units.isEmpty()) {
      units.put(UnitKind.LITTLE_FIGHTER, Math.max(1L, Math.round(fleetUnits * sizeFactor)));
    }

    var combatant = new Combatant(PIRATE_USER_ID, flight.getTargetCoordinates(), weaponsTechnology,
        shieldingTechnology, armorTechnology, units);
    return new PirateFleet(combatant);
  }

  private static void addScaledPirateUnits(EnumMap<UnitKind, Long> pirateUnits, Map<UnitKind, Integer> expeditionUnits,
                                           UnitKind unitKind, double sizeFactor, int minimum) {
    int expeditionCount = expeditionUnits.getOrDefault(unitKind, 0);
    if (expeditionCount <= 0) {
      return;
    }
    long pirateCount = Math.max(minimum, Math.round(expeditionCount * sizeFactor));
    pirateUnits.put(unitKind, pirateCount);
  }

  private static int randomPirateTechnology(int expeditionTechnology) {
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

  private record PirateFleet(Combatant combatant) {
  }
}
