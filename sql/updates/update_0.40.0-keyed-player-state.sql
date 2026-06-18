-- @formatter:off

-- Keyed player state (admin rebuild, phase 3).
--
-- Player state — building levels, unit counts and technology levels — used to
-- be stored in int[] columns indexed by Java enum ordinal. That made the data
-- positional: adding, removing or reordering an item shifted every value, and
-- the array length was pinned to the enum's size by a check constraint.
--
-- This migration converts those four columns to jsonb objects keyed by the
-- stable item name, e.g. {"METAL_MINE": 5}. An item absent from the object is
-- read as 0, so items introduced later need no backfill of existing rows.
--
-- The whole migration runs in one transaction: if any step fails, nothing is
-- applied.

begin;

-- bodies.buildings: int[18] indexed by BuildingKind ordinal -> jsonb keyed by name.
alter table bodies add column buildings_jsonb jsonb;
update bodies
   set buildings_jsonb = coalesce(
         (select jsonb_object_agg(name, buildings[ord])
            from unnest(array[
                   'METAL_MINE', 'CRYSTAL_MINE', 'DEUTERIUM_SYNTHESIZER', 'SOLAR_PLANT',
                   'FUSION_REACTOR', 'ROBOTICS_FACTORY', 'NANITE_FACTORY', 'SHIPYARD',
                   'METAL_STORAGE', 'CRYSTAL_STORAGE', 'DEUTERIUM_TANK', 'RESEARCH_LAB',
                   'TERRAFORMER', 'ALLIANCE_DEPOT', 'LUNAR_BASE', 'SENSOR_PHALANX',
                   'JUMP_GATE', 'MISSILE_SILO']) with ordinality as t(name, ord)),
         '{}'::jsonb);
alter table bodies drop column buildings;
alter table bodies rename column buildings_jsonb to buildings;
alter table bodies alter column buildings set not null;

-- bodies.units: int[23] indexed by UnitKind ordinal -> jsonb keyed by name.
alter table bodies add column units_jsonb jsonb;
update bodies
   set units_jsonb = coalesce(
         (select jsonb_object_agg(name, units[ord])
            from unnest(array[
                   'SMALL_CARGO', 'LARGE_CARGO', 'LITTLE_FIGHTER', 'HEAVY_FIGHTER',
                   'CRUISER', 'BATTLESHIP', 'COLONY_SHIP', 'RECYCLER', 'ESPIONAGE_PROBE',
                   'BOMBER', 'SOLAR_SATELLITE', 'DESTROYER', 'DEATH_STAR', 'ROCKET_LAUNCHER',
                   'LIGHT_LASER', 'HEAVY_LASER', 'GAIUS_CANNON', 'ION_CANNON', 'PLASMA_TURRET',
                   'SMALL_SHIELD_DOME', 'LARGE_SHIELD_DOME', 'ANTI_BALLISTIC_MISSILE',
                   'INTERPLANETARY_MISSILE']) with ordinality as t(name, ord)),
         '{}'::jsonb);
alter table bodies drop column units;
alter table bodies rename column units_jsonb to units;
alter table bodies alter column units set not null;

-- users.technologies: int[16] indexed by TechnologyKind ordinal -> jsonb keyed by name.
alter table users add column technologies_jsonb jsonb;
update users
   set technologies_jsonb = coalesce(
         (select jsonb_object_agg(name, technologies[ord])
            from unnest(array[
                   'ESPIONAGE_TECHNOLOGY', 'COMPUTER_TECHNOLOGY', 'WEAPONS_TECHNOLOGY',
                   'SHIELDING_TECHNOLOGY', 'ARMOR_TECHNOLOGY', 'ENERGY_TECHNOLOGY',
                   'HYPERSPACE_TECHNOLOGY', 'COMBUSTION_DRIVE', 'IMPULSE_DRIVE',
                   'HYPERSPACE_DRIVE', 'LASER_TECHNOLOGY', 'ION_TECHNOLOGY',
                   'PLASMA_TECHNOLOGY', 'INTERGALACTIC_RESEARCH_NETWORK', 'ASTROPHYSICS',
                   'GRAVITON_TECHNOLOGY']) with ordinality as t(name, ord)),
         '{}'::jsonb);
alter table users drop column technologies;
alter table users rename column technologies_jsonb to technologies;
alter table users alter column technologies set not null;

-- flights.units: int[23] indexed by UnitKind ordinal -> jsonb keyed by name.
-- The flight_view view selects flights.units, so it is dropped before the
-- column is replaced and recreated unchanged afterwards.
alter table flights add column units_jsonb jsonb;
update flights
   set units_jsonb = coalesce(
         (select jsonb_object_agg(name, units[ord])
            from unnest(array[
                   'SMALL_CARGO', 'LARGE_CARGO', 'LITTLE_FIGHTER', 'HEAVY_FIGHTER',
                   'CRUISER', 'BATTLESHIP', 'COLONY_SHIP', 'RECYCLER', 'ESPIONAGE_PROBE',
                   'BOMBER', 'SOLAR_SATELLITE', 'DESTROYER', 'DEATH_STAR', 'ROCKET_LAUNCHER',
                   'LIGHT_LASER', 'HEAVY_LASER', 'GAIUS_CANNON', 'ION_CANNON', 'PLASMA_TURRET',
                   'SMALL_SHIELD_DOME', 'LARGE_SHIELD_DOME', 'ANTI_BALLISTIC_MISSILE',
                   'INTERPLANETARY_MISSILE']) with ordinality as t(name, ord)),
         '{}'::jsonb);
drop view flight_view;
alter table flights drop column units;
alter table flights rename column units_jsonb to units;
alter table flights alter column units set not null;

create view flight_view as (
  select f.id,
         f.start_user_id,
         f.start_body_id,
         sb.galaxy as start_galaxy,
         sb.system as start_system,
         sb.position as start_position,
         sb.kind as start_kind,
         sb.name as start_body_name,
         f.target_user_id,
         f.target_body_id,
         f.target_galaxy,
         f.target_system,
         f.target_position,
         f.target_kind,
         f.party_id,
         f.departure_at,
         f.arrival_at,
         f.return_at,
         f.hold_until,
         f.mission,
         f.metal,
         f.crystal,
         f.deuterium,
         f.units
    from flights f
    join bodies sb
      on sb.id = f.start_body_id
);

commit;
