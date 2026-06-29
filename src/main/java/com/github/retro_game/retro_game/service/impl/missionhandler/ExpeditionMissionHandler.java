package com.github.retro_game.retro_game.service.impl.missionhandler;

import com.github.retro_game.retro_game.battleengine.BattleEngine;
import com.github.retro_game.retro_game.battleengine.BattleOutcome;
import com.github.retro_game.retro_game.battleengine.Combatant;
import com.github.retro_game.retro_game.battleengine.CombatantOutcome;
import com.github.retro_game.retro_game.battleengine.UnitGroupStats;
import com.github.retro_game.retro_game.dto.MoonCreationResultDto;
import com.github.retro_game.retro_game.entity.BattleResult;
import com.github.retro_game.retro_game.entity.Body;
import com.github.retro_game.retro_game.entity.DebrisField;
import com.github.retro_game.retro_game.entity.DebrisFieldKey;
import com.github.retro_game.retro_game.entity.Event;
import com.github.retro_game.retro_game.entity.EventKind;
import com.github.retro_game.retro_game.entity.ExpeditionEventType;
import com.github.retro_game.retro_game.entity.Flight;
import com.github.retro_game.retro_game.entity.Resources;
import com.github.retro_game.retro_game.entity.TechnologyKind;
import com.github.retro_game.retro_game.entity.UnitKind;
import com.github.retro_game.retro_game.model.ItemCostUtils;
import com.github.retro_game.retro_game.repository.DebrisFieldRepository;
import com.github.retro_game.retro_game.repository.FlightRepository;
import com.github.retro_game.retro_game.service.ActivityService;
import com.github.retro_game.retro_game.service.impl.BodyServiceInternal;
import com.github.retro_game.retro_game.service.impl.CombatReportServiceInternal;
import com.github.retro_game.retro_game.service.impl.EventScheduler;
import com.github.retro_game.retro_game.service.impl.ReportServiceInternal;
import com.github.retro_game.retro_game.service.impl.UnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class ExpeditionMissionHandler {
    private static final long PIRATE_USER_ID = -1L;
    private static final long ALIEN_USER_ID = -2L;
    private static final String PIRATE_USER_NAME = "Pirates";
    private static final String ALIEN_USER_NAME = "Aliens";
    private static final Logger logger = LoggerFactory.getLogger(ExpeditionMissionHandler.class);
    private static final String NOTHING_REPORT_KEY = "otherReportExpedition.nothing";
    private static final String ONE_HOUR_DELAY_REPORT_KEY = "otherReportExpedition.delay.oneHour";
    private static final String TWO_HOURS_DELAY_REPORT_KEY = "otherReportExpedition.delay.twoHours";
    private static final String THREE_HOURS_DELAY_REPORT_KEY = "otherReportExpedition.delay.threeHours";
    private static final String FLEET_LOSS_REPORT_KEY = "otherReportExpedition.fleetLoss";
    private static final String PIRATES_REPORT_KEY = "otherReportExpedition.pirates";
    private static final String ALIENS_REPORT_KEY = "otherReportExpedition.aliens";
    private static final String ORE_ASTEROID_REPORT_KEY = "otherReportExpedition.oreAsteroid";
    private static final String GAS_CLOUD_REPORT_KEY = "otherReportExpedition.gasCloud";
    private static final String SPECTACULAR_SUPERNOVA_REPORT_KEY = "otherReportExpedition.spectacularSupernova";
    private static final String WARP_WINDOW_REPORT_KEY = "otherReportExpedition.warpWindow";
    private static final String RESCUE_SHIPS_REPORT_KEY = "otherReportExpedition.rescueShips";
    private static final String RESCUE_FLEET_REPORT_KEY = "otherReportExpedition.rescueFleet";
    private static final List<UnitKind> EXPEDITION_ENCOUNTER_SHIPS = List.of(
            UnitKind.ESPIONAGE_PROBE,
            UnitKind.SMALL_CARGO,
            UnitKind.LARGE_CARGO,
            UnitKind.RECYCLER,
            UnitKind.LITTLE_FIGHTER,
            UnitKind.HEAVY_FIGHTER,
            UnitKind.CRUISER,
            UnitKind.BATTLESHIP,
            UnitKind.BATTLE_CRUISER,
            UnitKind.BOMBER,
            UnitKind.DESTROYER,
            UnitKind.DEATH_STAR
    );

    private final ActivityService activityService;
    private final BattleEngine battleEngine;
    private final BodyServiceInternal bodyServiceInternal;
    private final CombatReportServiceInternal combatReportServiceInternal;
    private final DebrisFieldRepository debrisFieldRepository;
    private final EventScheduler eventScheduler;
    private final FlightRepository flightRepository;
    private final double fleetDebrisFactor;
    private final MessageSource messageSource;
    private final MissionHandlerUtils missionHandlerUtils;
    private final ReportServiceInternal reportServiceInternal;
    private final UnitService unitService;

    public ExpeditionMissionHandler(ActivityService activityService, BattleEngine battleEngine,
                                    BodyServiceInternal bodyServiceInternal,
                                    CombatReportServiceInternal combatReportServiceInternal,
                                    DebrisFieldRepository debrisFieldRepository, EventScheduler eventScheduler,
                                    FlightRepository flightRepository,
                                    @Value("${retro-game.fleet-debris-factor:0.3}") double fleetDebrisFactor,
                                    MessageSource messageSource,
                                    MissionHandlerUtils missionHandlerUtils, ReportServiceInternal reportServiceInternal,
                                    UnitService unitService) {
        this.activityService = activityService;
        this.battleEngine = battleEngine;
        this.bodyServiceInternal = bodyServiceInternal;
        this.combatReportServiceInternal = combatReportServiceInternal;
        this.debrisFieldRepository = debrisFieldRepository;
        this.eventScheduler = eventScheduler;
        this.flightRepository = flightRepository;
        this.fleetDebrisFactor = fleetDebrisFactor;
        this.messageSource = messageSource;
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
        switch (eventType) {
            case Nothing ->
                    reportServiceInternal.createExpeditionReport(flight, getMessage(flight, NOTHING_REPORT_KEY));
            case Delay -> handleDelay(flight);
            case FleetLoss -> {
                handleFleetLoss(flight);
                return;
            }
            case Pirates -> {
                if (!handlePirates(flight)) {
                    return;
                }
            }
            case Aliens -> {
                if (!handleAliens(flight)) {
                    return;
                }
            }
            case OreAsteroid -> handleOreAsteroid(flight);
            case GasCloud -> handleGasCloud(flight);
            case SpectacularSupernova ->
                    reportServiceInternal.createExpeditionReport(flight, getMessage(flight, SPECTACULAR_SUPERNOVA_REPORT_KEY));
            case WarpWindow -> {
                handleWarpWindow(flight);
                return;
            }
            case RescueShips -> handleRescueShips(flight);
            case RescueFleet -> handleRescueFleet(flight);
        }
        missionHandlerUtils.scheduleReturn(flight);
    }

    private void handleFleetLoss(Flight flight) {
        reportServiceInternal.createExpeditionReport(flight, getMessage(flight, FLEET_LOSS_REPORT_KEY));
        flightRepository.delete(flight);
    }

    private boolean handlePirates(Flight flight) {
        return handleHostileEncounter(
                flight,
                getMessage(flight, PIRATES_REPORT_KEY),
                calculateEncounterFleet(
                        flight,
                        PIRATE_USER_ID,
                        PIRATE_USER_NAME,
                        ThreadLocalRandom.current().nextDouble(0.01, 0.1),
                        false
                )
        );
    }

    private boolean handleAliens(Flight flight) {
        return handleHostileEncounter(
                flight,
                getMessage(flight, ALIENS_REPORT_KEY),
                calculateEncounterFleet(
                        flight,
                        ALIEN_USER_ID,
                        ALIEN_USER_NAME,
                        ThreadLocalRandom.current().nextDouble(0.25, 0.69),
                        true
                )
        );
    }

    private void handleRescueShips(Flight flight) {
        var rescuedFleet = calculateEncounterFleet(
                flight,
                0L,
                "Rescued ships",
                ThreadLocalRandom.current().nextDouble(0.01, 0.04),
                false
        );
        addRescuedShips(flight, rescuedFleet.combatant().unitGroups());
        reportServiceInternal.createExpeditionReport(
                flight,
                getMessage(
                        flight,
                        RESCUE_SHIPS_REPORT_KEY,
                        formatFoundShips(flight, rescuedFleet.combatant().unitGroups())
                )
        );
    }

    private void handleRescueFleet(Flight flight) {
        var rescuedFleet = calculateEncounterFleet(
                flight,
                0L,
                "Rescued fleet",
                ThreadLocalRandom.current().nextDouble(0.05, 0.15),
                false
        );
        addRescuedShips(flight, rescuedFleet.combatant().unitGroups());
        reportServiceInternal.createExpeditionReport(
                flight,
                getMessage(
                        flight,
                        RESCUE_FLEET_REPORT_KEY,
                        formatFoundShips(flight, rescuedFleet.combatant().unitGroups())
                )
        );
    }

    private void handleOreAsteroid(Flight flight) {
        long maxResources = (long) Math.floor(
                getRemainingCargoSpace(flight) * ThreadLocalRandom.current().nextDouble(0.05, 0.21)
        );
        long totalResources = pickResourceAmount(maxResources);
        long metal = totalResources == 0 ? 0 : ThreadLocalRandom.current().nextLong(totalResources + 1);
        long crystal = totalResources - metal;

        Resources resources = flight.getResources();
        resources.setMetal(resources.getMetal() + metal);
        resources.setCrystal(resources.getCrystal() + crystal);
        reportServiceInternal.createExpeditionReport(flight, getMessage(flight, ORE_ASTEROID_REPORT_KEY, metal, crystal));
    }

    private void handleGasCloud(Flight flight) {
        long maxDeuterium = (long) Math.floor(
                getRemainingCargoSpace(flight) * ThreadLocalRandom.current().nextDouble(0.01, 0.13)
        );
        long deuterium = pickResourceAmount(maxDeuterium);

        Resources resources = flight.getResources();
        resources.setDeuterium(resources.getDeuterium() + deuterium);
        reportServiceInternal.createExpeditionReport(flight, getMessage(flight, GAS_CLOUD_REPORT_KEY, deuterium));
    }

    private void handleWarpWindow(Flight flight) {
        flight.setReturnAt(flight.getHoldUntil());
        reportServiceInternal.createExpeditionReport(flight, getMessage(flight, WARP_WINDOW_REPORT_KEY));

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
        Resources debris = calcDebris(attackersLoss, defendersLoss);
        createOrUpdateDebrisField(flight, debris);
        var emptyResources = new Resources();
        var moonCreationResult = new MoonCreationResultDto(0.0, false);
        var combatReport = combatReportServiceInternal.create(flight.getHoldUntil(), attackers, defenders, battleOutcome,
                battleResult, attackersLoss, defendersLoss, emptyResources, debris, moonCreationResult, null, seed,
                executionTime);
        reportServiceInternal.createSimplifiedCombatReport(flight.getStartUser(), true, flight.getHoldUntil(), (Long) null,
                hostileFleet.name(), flight.getTargetCoordinates(), battleResult, battleOutcome.numRounds(), attackersLoss,
                defendersLoss, emptyResources, debris, moonCreationResult, combatReport);

        applyRemainingUnits(flight, attackerStats);
        if (flight.getTotalUnitsCount() == 0) {
            flightRepository.delete(flight);
            return false;
        }
        return true;
    }

    private Resources calcDebris(Resources attackersLoss, Resources defendersLoss) {
        var debris = new Resources(attackersLoss);
        debris.add(defendersLoss);
        debris.mul(fleetDebrisFactor);
        debris.setDeuterium(0.0);
        debris.floor();
        assert debris.isNonNegative();
        return debris;
    }

    private void createOrUpdateDebrisField(Flight flight, Resources debris) {
        var metal = (long) debris.getMetal();
        var crystal = (long) debris.getCrystal();
        if (metal == 0 && crystal == 0) {
            return;
        }

        var coords = flight.getTargetCoordinates();
        var key = new DebrisFieldKey(coords.getGalaxy(), coords.getSystem(), coords.getPosition());
        var dfOpt = debrisFieldRepository.findById(key);
        if (dfOpt.isEmpty()) {
            var df = new DebrisField(key, flight.getHoldUntil(), flight.getHoldUntil(), metal, crystal);
            debrisFieldRepository.save(df);
        } else {
            var df = dfOpt.get();
            df.setUpdatedAt(flight.getHoldUntil());
            df.setMetal(df.getMetal() + metal);
            df.setCrystal(df.getCrystal() + crystal);
        }
    }

    private HostileFleet calculateEncounterFleet(
            Flight flight,
            long userId,
            String userName,
            double sizeFactor,
            boolean strongerTechnology
    ) {
        int weaponsTechnology = randomEncounterTechnology(
                flight.getStartUser().getTechnologyLevel(TechnologyKind.WEAPONS_TECHNOLOGY), strongerTechnology);
        int shieldingTechnology = randomEncounterTechnology(
                flight.getStartUser().getTechnologyLevel(TechnologyKind.SHIELDING_TECHNOLOGY), strongerTechnology);
        int armorTechnology = randomEncounterTechnology(
                flight.getStartUser().getTechnologyLevel(TechnologyKind.ARMOR_TECHNOLOGY), strongerTechnology);

        var units = new EnumMap<UnitKind, Long>(UnitKind.class);
        int highestEncounterShipIndex = getHighestEncounterShipIndex(flight.getUnits());
        long fleetValue = calculateFleetMetalCrystalValue(flight.getUnits());
        long targetValue = Math.max(1L, Math.round(fleetValue * sizeFactor));
        if (highestEncounterShipIndex > 0) {
            addEncounterShips(units, targetValue, highestEncounterShipIndex);
        }

        if (units.isEmpty()) {
            units.put(UnitKind.ESPIONAGE_PROBE, calculateUnitCount(UnitKind.ESPIONAGE_PROBE, targetValue));
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

    private String formatFoundShips(Flight flight, Map<UnitKind, Long> ships) {
        StringBuilder builder = new StringBuilder();
        Locale locale = getLocale(flight);
        for (UnitKind kind : UnitKind.values()) {
            long count = ships.getOrDefault(kind, 0L);
            if (count <= 0) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(count).append(' ').append(messageSource.getMessage("items." + kind + ".name", null, locale));
        }
        return builder.toString();
    }

    private String getMessage(Flight flight, String key, Object... args) {
        return messageSource.getMessage(key, args.length == 0 ? null : args, getLocale(flight));
    }

    private static Locale getLocale(Flight flight) {
        return Locale.forLanguageTag(flight.getStartUser().getLanguage());
    }

    private static int getHighestEncounterShipIndex(Map<UnitKind, Integer> expeditionUnits) {
        int highestIndex = -1;
        for (int i = 0; i < EXPEDITION_ENCOUNTER_SHIPS.size(); i++) {
            if (expeditionUnits.getOrDefault(EXPEDITION_ENCOUNTER_SHIPS.get(i), 0) > 0) {
                highestIndex = i;
            }
        }
        return highestIndex;
    }

    private static long calculateFleetMetalCrystalValue(Map<UnitKind, Integer> units) {
        long value = 0L;
        for (var entry : units.entrySet()) {
            value += entry.getValue() * getMetalCrystalValue(entry.getKey());
        }
        return value;
    }

    private static long getMetalCrystalValue(UnitKind kind) {
        Resources cost = ItemCostUtils.getCost(kind);
        return Math.round(cost.getMetal() + cost.getCrystal());
    }

    private static long calculateUnitCount(UnitKind kind, long targetValue) {
        long unitValue = getMetalCrystalValue(kind);
        if (unitValue <= 0) {
            return 1L;
        }
        return Math.max(1L, Math.round((double) targetValue / unitValue));
    }

    private static void addEncounterShips(EnumMap<UnitKind, Long> hostileUnits, long targetValue,
                                          int maxExclusiveIndex) {
        if (targetValue <= 0 || maxExclusiveIndex <= 0) {
            return;
        }

        int offset = ThreadLocalRandom.current().nextInt(maxExclusiveIndex);
        long remainingValue = targetValue;
        for (int i = 0; i < maxExclusiveIndex; i++) {
            UnitKind unitKind = EXPEDITION_ENCOUNTER_SHIPS.get((offset + i) % maxExclusiveIndex);
            long unitValue = getMetalCrystalValue(unitKind);
            if (unitValue <= 0 || remainingValue < unitValue) {
                continue;
            }

            long maxUnits = remainingValue / unitValue;
            long units = hasAffordableEncounterShipAfter(remainingValue, i, offset, maxExclusiveIndex)
                    ? ThreadLocalRandom.current().nextLong(1, maxUnits + 1)
                    : maxUnits;
            hostileUnits.put(unitKind, units);
            remainingValue -= units * unitValue;
        }
    }

    private static boolean hasAffordableEncounterShipAfter(long value, int index, int offset, int maxExclusiveIndex) {
        for (int i = index + 1; i < maxExclusiveIndex; i++) {
            UnitKind unitKind = EXPEDITION_ENCOUNTER_SHIPS.get((offset + i) % maxExclusiveIndex);
            long unitValue = getMetalCrystalValue(unitKind);
            if (unitValue > 0 && value >= unitValue) {
                return true;
            }
        }
        return false;
    }

    private static int randomEncounterTechnology(int expeditionTechnology, boolean strongerTechnology) {
        if (strongerTechnology) {
            return expeditionTechnology + ThreadLocalRandom.current().nextInt(1, 4);
        }
        return Math.max(0, expeditionTechnology - ThreadLocalRandom.current().nextInt(4));
    }

    private static Combatant makeExpeditionCombatant(Flight flight) {
        var user = flight.getStartUser();
        return new Combatant(
                user.getId(),
                flight.getStartBody().getCoordinates(),
                user.getTechnologyLevel(TechnologyKind.WEAPONS_TECHNOLOGY),
                user.getTechnologyLevel(TechnologyKind.SHIELDING_TECHNOLOGY),
                user.getTechnologyLevel(TechnologyKind.ARMOR_TECHNOLOGY),
                makeUnitGroups(flight.getUnits())
        );
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

        String reportKey = switch (delayHours) {
            case 1 -> ONE_HOUR_DELAY_REPORT_KEY;
            case 2 -> TWO_HOURS_DELAY_REPORT_KEY;
            default -> THREE_HOURS_DELAY_REPORT_KEY;
        };
        reportServiceInternal.createExpeditionReport(flight, getMessage(flight, reportKey));
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
