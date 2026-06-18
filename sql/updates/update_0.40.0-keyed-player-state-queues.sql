-- @formatter:off

-- Keyed player state — construction queues (admin rebuild, phase 4.5).
--
-- The three construction queues used to be packed into int[]/bigint[] columns,
-- with each entry's item kind (and action) stored as a Java enum ordinal():
--
--   bodies.building_queue    int[]    — 3 ints per entry:
--                                       [sequence, BuildingKind, BuildingQueueAction]
--   bodies.shipyard_queue    int[]    — 2 ints per entry:
--                                       [UnitKind, count]
--   users.technology_queue   bigint[] — 3 longs per entry:
--                                       [sequence, TechnologyKind, bodyId]
--
-- Storing the kind as an ordinal made the data positional: reordering an enum
-- constant silently remapped every queued item to the wrong kind.
--
-- This migration converts the three columns to jsonb arrays of objects whose
-- kind/action are the stable item NAME strings, e.g.
--   [{"sequence": 1, "kind": "METAL_MINE", "action": "CONSTRUCT"}]
-- Each old ordinal is mapped to its name through an inline array of the enum's
-- constants, in declaration order. Postgres arrays are 1-based and Java enum
-- ordinals are 0-based, so the name array is indexed with (ordinal + 1). An
-- empty queue becomes [].
--
-- This mirrors update_0.40.0-keyed-player-state.sql, which converted the
-- building-level / unit-count / technology-level columns.
--
-- The whole migration runs in one transaction: if any step fails, nothing is
-- applied.

begin;

-- bodies.building_queue: int[] (3 ints per entry: sequence, BuildingKind
-- ordinal, BuildingQueueAction ordinal) -> jsonb array of objects keyed by name.
alter table bodies add column building_queue_jsonb jsonb;
update bodies
   set building_queue_jsonb = coalesce(
         (select jsonb_agg(
                   jsonb_build_object(
                     'sequence', building_queue[3 * i + 1],
                     'kind',     (array[
                                    'METAL_MINE', 'CRYSTAL_MINE', 'DEUTERIUM_SYNTHESIZER',
                                    'SOLAR_PLANT', 'FUSION_REACTOR', 'ROBOTICS_FACTORY',
                                    'NANITE_FACTORY', 'SHIPYARD', 'METAL_STORAGE',
                                    'CRYSTAL_STORAGE', 'DEUTERIUM_TANK', 'RESEARCH_LAB',
                                    'TERRAFORMER', 'ALLIANCE_DEPOT', 'LUNAR_BASE',
                                    'SENSOR_PHALANX', 'JUMP_GATE', 'MISSILE_SILO'
                                  ])[building_queue[3 * i + 2] + 1],
                     'action',   (array[
                                    'CONSTRUCT', 'DESTROY'
                                  ])[building_queue[3 * i + 3] + 1])
                   order by i)
            from generate_series(0, array_length(building_queue, 1) / 3 - 1) as i),
         '[]'::jsonb);
alter table bodies drop column building_queue;
alter table bodies rename column building_queue_jsonb to building_queue;
alter table bodies alter column building_queue set not null;

-- bodies.shipyard_queue: int[] (2 ints per entry: UnitKind ordinal, count) ->
-- ordered jsonb array of objects keyed by name.
alter table bodies add column shipyard_queue_jsonb jsonb;
update bodies
   set shipyard_queue_jsonb = coalesce(
         (select jsonb_agg(
                   jsonb_build_object(
                     'kind',  (array[
                                 'SMALL_CARGO', 'LARGE_CARGO', 'LITTLE_FIGHTER',
                                 'HEAVY_FIGHTER', 'CRUISER', 'BATTLESHIP', 'COLONY_SHIP',
                                 'RECYCLER', 'ESPIONAGE_PROBE', 'BOMBER', 'SOLAR_SATELLITE',
                                 'DESTROYER', 'DEATH_STAR', 'ROCKET_LAUNCHER', 'LIGHT_LASER',
                                 'HEAVY_LASER', 'GAIUS_CANNON', 'ION_CANNON', 'PLASMA_TURRET',
                                 'SMALL_SHIELD_DOME', 'LARGE_SHIELD_DOME',
                                 'ANTI_BALLISTIC_MISSILE', 'INTERPLANETARY_MISSILE'
                               ])[shipyard_queue[2 * i + 1] + 1],
                     'count', shipyard_queue[2 * i + 2])
                   order by i)
            from generate_series(0, array_length(shipyard_queue, 1) / 2 - 1) as i),
         '[]'::jsonb);
alter table bodies drop column shipyard_queue;
alter table bodies rename column shipyard_queue_jsonb to shipyard_queue;
alter table bodies alter column shipyard_queue set not null;

-- users.technology_queue: bigint[] (3 longs per entry: sequence, TechnologyKind
-- ordinal, bodyId) -> jsonb array of objects keyed by name.
alter table users add column technology_queue_jsonb jsonb;
update users
   set technology_queue_jsonb = coalesce(
         (select jsonb_agg(
                   jsonb_build_object(
                     'sequence', technology_queue[3 * i + 1],
                     'kind',     (array[
                                    'ESPIONAGE_TECHNOLOGY', 'COMPUTER_TECHNOLOGY',
                                    'WEAPONS_TECHNOLOGY', 'SHIELDING_TECHNOLOGY',
                                    'ARMOR_TECHNOLOGY', 'ENERGY_TECHNOLOGY',
                                    'HYPERSPACE_TECHNOLOGY', 'COMBUSTION_DRIVE',
                                    'IMPULSE_DRIVE', 'HYPERSPACE_DRIVE', 'LASER_TECHNOLOGY',
                                    'ION_TECHNOLOGY', 'PLASMA_TECHNOLOGY',
                                    'INTERGALACTIC_RESEARCH_NETWORK', 'ASTROPHYSICS',
                                    'GRAVITON_TECHNOLOGY'
                                  ])[technology_queue[3 * i + 2] + 1],
                     'bodyId',   technology_queue[3 * i + 3])
                   order by i)
            from generate_series(0, array_length(technology_queue, 1) / 3 - 1) as i),
         '[]'::jsonb);
alter table users drop column technology_queue;
alter table users rename column technology_queue_jsonb to technology_queue;
alter table users alter column technology_queue set not null;

commit;
