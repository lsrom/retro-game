-- @formatter:off

-- Content catalog seed (admin rebuild, stage 6).
--
-- The 57 built-in items — 18 buildings, 16 technologies, 23 units — and their
-- 100 build requirements. Before stage 6 the catalog was seeded at start-up
-- from the hardcoded Java item classes under model/; those classes have now
-- been removed, so this script is the authoritative source of the built-in
-- content instead.
--
-- CatalogSeeder runs this on first start, when item_definitions is empty (it
-- loads it as a classpath resource — a copy lives at
-- src/main/resources/catalog-seed.sql — and executes it through the
-- DataSource). Ids are inserted explicitly so item_requirements can reference
-- them by id; the setval calls at the end advance the id sequences past the
-- seeded rows, so items later created through the admin panel get fresh ids.
--
-- This script was generated from a correctly seeded database with:
--   pg_dump -U postgres -d retro-game --data-only --column-inserts \
--           --table=item_definitions --table=item_requirements
-- The INSERTs are sorted by id for readability; order does not matter, as the
-- game reads catalog items ordered by id.

--
-- Data for Name: item_definitions
--

INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (1, 'BUILDING', 'METAL_MINE', 'Metal Mine', 60, 15, 0, 1.5, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (2, 'BUILDING', 'CRYSTAL_MINE', 'Crystal Mine', 48, 24, 0, 1.6, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (3, 'BUILDING', 'DEUTERIUM_SYNTHESIZER', 'Deuterium Synthesizer', 225, 75, 0, 1.5, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (4, 'BUILDING', 'SOLAR_PLANT', 'Solar Plant', 75, 30, 0, 1.5, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (5, 'BUILDING', 'FUSION_REACTOR', 'Fusion Reactor', 900, 360, 180, 1.8, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (6, 'BUILDING', 'ROBOTICS_FACTORY', 'Robotics Factory', 400, 120, 200, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (7, 'BUILDING', 'NANITE_FACTORY', 'Nanite Factory', 1000000, 500000, 100000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (8, 'BUILDING', 'SHIPYARD', 'Shipyard', 400, 200, 100, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (9, 'BUILDING', 'METAL_STORAGE', 'Metal Storage', 2000, 0, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (10, 'BUILDING', 'CRYSTAL_STORAGE', 'Crystal Storage', 2000, 1000, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (11, 'BUILDING', 'DEUTERIUM_TANK', 'Deuterium Tank', 2000, 2000, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (12, 'BUILDING', 'RESEARCH_LAB', 'Research Lab', 200, 400, 200, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (13, 'BUILDING', 'TERRAFORMER', 'Terraformer', 0, 50000, 100000, 2, 1000, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (14, 'BUILDING', 'ALLIANCE_DEPOT', 'Alliance Depot', 20000, 40000, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (15, 'BUILDING', 'LUNAR_BASE', 'Lunar Base', 20000, 40000, 20000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (16, 'BUILDING', 'SENSOR_PHALANX', 'Sensor Phalanx', 20000, 40000, 20000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (17, 'BUILDING', 'JUMP_GATE', 'Jump Gate', 2000000, 4000000, 2000000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (18, 'BUILDING', 'MISSILE_SILO', 'Missile Silo', 20000, 20000, 1000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (19, 'TECHNOLOGY', 'ESPIONAGE_TECHNOLOGY', 'Espionage Technology', 200, 1000, 200, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (20, 'TECHNOLOGY', 'COMPUTER_TECHNOLOGY', 'Computer Technology', 0, 400, 600, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (21, 'TECHNOLOGY', 'WEAPONS_TECHNOLOGY', 'Weapons Technology', 800, 200, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (22, 'TECHNOLOGY', 'SHIELDING_TECHNOLOGY', 'Shielding Technology', 200, 600, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (23, 'TECHNOLOGY', 'ARMOR_TECHNOLOGY', 'Armor Technology', 1000, 0, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (24, 'TECHNOLOGY', 'ENERGY_TECHNOLOGY', 'Energy Technology', 0, 800, 400, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (25, 'TECHNOLOGY', 'HYPERSPACE_TECHNOLOGY', 'Hyperspace Technology', 0, 4000, 2000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (26, 'TECHNOLOGY', 'COMBUSTION_DRIVE', 'Combustion Drive', 400, 0, 600, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (27, 'TECHNOLOGY', 'IMPULSE_DRIVE', 'Impulse Drive', 2000, 4000, 600, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (28, 'TECHNOLOGY', 'HYPERSPACE_DRIVE', 'Hyperspace Drive', 10000, 20000, 6000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (29, 'TECHNOLOGY', 'LASER_TECHNOLOGY', 'Laser Technology', 200, 100, 0, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (30, 'TECHNOLOGY', 'ION_TECHNOLOGY', 'Ion Technology', 1000, 300, 100, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (31, 'TECHNOLOGY', 'PLASMA_TECHNOLOGY', 'Plasma Technology', 2000, 4000, 1000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (32, 'TECHNOLOGY', 'INTERGALACTIC_RESEARCH_NETWORK', 'Intergalactic Research Network', 240000, 400000, 160000, 2, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (33, 'TECHNOLOGY', 'ASTROPHYSICS', 'Astrophysics', 4000, 8000, 4000, 1.75, 0, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (34, 'TECHNOLOGY', 'GRAVITON_TECHNOLOGY', 'Graviton Technology', 0, 0, 0, 3, 300000, NULL, 0, 0, 0, 0);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (35, 'UNIT', 'SMALL_CARGO', 'Small Cargo', 2000, 2000, 0, 1, 0, 'FLEET', 5000, 5, 10, 4000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (36, 'UNIT', 'LARGE_CARGO', 'Large Cargo', 6000, 6000, 0, 1, 0, 'FLEET', 25000, 5, 25, 12000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (37, 'UNIT', 'LITTLE_FIGHTER', 'Little Fighter', 3000, 1000, 0, 1, 0, 'FLEET', 50, 50, 10, 4000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (38, 'UNIT', 'HEAVY_FIGHTER', 'Heavy Fighter', 6000, 4000, 0, 1, 0, 'FLEET', 100, 150, 25, 10000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (39, 'UNIT', 'CRUISER', 'Cruiser', 20000, 7000, 2000, 1, 0, 'FLEET', 800, 400, 50, 27000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (40, 'UNIT', 'BATTLESHIP', 'Battleship', 40000, 20000, 0, 1, 0, 'FLEET', 1500, 1000, 200, 60000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (41, 'UNIT', 'COLONY_SHIP', 'Colony Ship', 10000, 20000, 10000, 1, 0, 'FLEET', 7500, 50, 100, 30000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (42, 'UNIT', 'RECYCLER', 'Recycler', 10000, 6000, 2000, 1, 0, 'FLEET', 20000, 1, 10, 16000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (43, 'UNIT', 'ESPIONAGE_PROBE', 'Espionage Probe', 0, 1000, 0, 1, 0, 'FLEET', 5, 0.01, 0.01, 1000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (44, 'UNIT', 'BOMBER', 'Bomber', 50000, 25000, 15000, 1, 0, 'FLEET', 500, 1000, 500, 75000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (45, 'UNIT', 'SOLAR_SATELLITE', 'Solar Satellite', 0, 2000, 500, 1, 0, 'FLEET', 0, 1, 1, 2000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (46, 'UNIT', 'DESTROYER', 'Destroyer', 60000, 50000, 15000, 1, 0, 'FLEET', 2000, 2000, 500, 110000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (47, 'UNIT', 'DEATH_STAR', 'Death Star', 5000000, 4000000, 1000000, 1, 0, 'FLEET', 1000000, 200000, 50000, 9000000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (48, 'UNIT', 'ROCKET_LAUNCHER', 'Rocket Launcher', 2000, 0, 0, 1, 0, 'DEFENSE', 0, 80, 20, 2000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (49, 'UNIT', 'LIGHT_LASER', 'Light Laser', 1500, 500, 0, 1, 0, 'DEFENSE', 0, 100, 25, 2000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (50, 'UNIT', 'HEAVY_LASER', 'Heavy Laser', 6000, 2000, 0, 1, 0, 'DEFENSE', 0, 250, 100, 8000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (51, 'UNIT', 'GAIUS_CANNON', 'Gaius Cannon', 20000, 15000, 2000, 1, 0, 'DEFENSE', 0, 1100, 200, 35000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (52, 'UNIT', 'ION_CANNON', 'Ion Cannon', 2000, 6000, 0, 1, 0, 'DEFENSE', 0, 150, 500, 8000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (53, 'UNIT', 'PLASMA_TURRET', 'Plasma Turret', 50000, 50000, 30000, 1, 0, 'DEFENSE', 0, 3000, 300, 100000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (54, 'UNIT', 'SMALL_SHIELD_DOME', 'Small Shield Dome', 10000, 10000, 0, 1, 0, 'DEFENSE', 0, 1, 2000, 20000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (55, 'UNIT', 'LARGE_SHIELD_DOME', 'Large Shield Dome', 50000, 50000, 0, 1, 0, 'DEFENSE', 0, 1, 10000, 100000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (56, 'UNIT', 'ANTI_BALLISTIC_MISSILE', 'Anti-ballistic Missile', 8000, 0, 2000, 1, 0, 'DEFENSE', 0, 1, 1, 8000);
INSERT INTO public.item_definitions (id, type, kind, name, metal_cost, crystal_cost, deuterium_cost, cost_factor, base_energy, unit_type, capacity, weapons, shield, armor) VALUES (57, 'UNIT', 'INTERPLANETARY_MISSILE', 'Interplanetary Missile', 12500, 2500, 10000, 1, 0, 'DEFENSE', 0, 12000, 1, 15000);

--
-- Data for Name: item_requirements
--

INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (1, 5, 24, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (2, 5, 3, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (3, 7, 20, 10);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (4, 7, 6, 10);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (5, 8, 6, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (6, 13, 24, 12);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (7, 13, 7, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (8, 16, 15, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (9, 17, 15, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (10, 17, 25, 7);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (11, 18, 8, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (12, 19, 12, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (13, 20, 12, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (14, 21, 12, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (15, 22, 24, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (16, 22, 12, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (17, 23, 12, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (18, 24, 12, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (19, 25, 24, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (20, 25, 12, 7);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (21, 25, 22, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (22, 26, 24, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (23, 26, 12, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (24, 27, 24, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (25, 27, 12, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (26, 28, 12, 7);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (27, 28, 25, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (28, 29, 24, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (29, 29, 12, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (30, 30, 24, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (31, 30, 29, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (32, 30, 12, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (33, 31, 24, 8);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (34, 31, 29, 10);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (35, 31, 12, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (36, 31, 30, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (37, 32, 20, 8);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (38, 32, 12, 10);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (39, 32, 25, 8);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (40, 33, 12, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (41, 33, 27, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (42, 33, 19, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (43, 34, 12, 12);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (44, 35, 8, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (45, 35, 26, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (46, 36, 8, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (47, 36, 26, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (48, 37, 8, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (49, 37, 26, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (50, 38, 23, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (51, 38, 8, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (52, 38, 27, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (53, 39, 8, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (54, 39, 27, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (55, 39, 30, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (56, 40, 8, 7);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (57, 40, 28, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (58, 41, 8, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (59, 41, 27, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (60, 42, 8, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (61, 42, 22, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (62, 42, 26, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (63, 43, 8, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (64, 43, 19, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (65, 43, 26, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (66, 44, 8, 8);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (67, 44, 27, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (68, 44, 31, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (69, 45, 8, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (70, 46, 8, 9);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (71, 46, 28, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (72, 46, 25, 5);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (73, 47, 8, 12);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (74, 47, 34, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (75, 47, 28, 7);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (76, 47, 25, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (77, 48, 8, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (78, 49, 8, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (79, 49, 24, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (80, 49, 29, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (81, 50, 8, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (82, 50, 24, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (83, 50, 29, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (84, 51, 8, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (85, 51, 24, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (86, 51, 21, 3);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (87, 51, 22, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (88, 52, 8, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (89, 52, 30, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (90, 53, 8, 8);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (91, 53, 31, 7);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (92, 54, 8, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (93, 54, 22, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (94, 55, 8, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (95, 55, 22, 6);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (96, 56, 8, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (97, 56, 18, 2);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (98, 57, 8, 1);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (99, 57, 18, 4);
INSERT INTO public.item_requirements (id, item_id, required_item_id, required_level) VALUES (100, 57, 27, 1);

--
-- Advance the id sequences past the seeded rows, so items created later
-- through the admin panel get fresh ids. (Emitted by pg_dump.)
--

SELECT pg_catalog.setval('public.item_definitions_id_seq', 59, true);
SELECT pg_catalog.setval('public.item_requirements_id_seq', 101, true);
