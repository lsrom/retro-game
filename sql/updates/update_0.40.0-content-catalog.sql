-- @formatter:off

-- Content catalog (admin rebuild, phase 1).
--
-- Adds the tables that will hold every building, technology and unit
-- definition. At this phase the game still runs off the hardcoded Java item
-- classes; these tables are seeded from those values and edited via the admin
-- panel. Later phases make the game read its content from here.

create table item_definitions (
  id bigserial primary key,
  type text not null check (type in ('BUILDING', 'TECHNOLOGY', 'UNIT')),
  -- Stable identifier, e.g. 'METAL_MINE'. Matches the legacy enum constant for seeded items.
  kind text not null unique,
  name text not null,
  metal_cost double precision not null default 0 check (metal_cost >= 0),
  crystal_cost double precision not null default 0 check (crystal_cost >= 0),
  deuterium_cost double precision not null default 0 check (deuterium_cost >= 0),
  -- Buildings and technologies: cost grows by cost_factor^(level-1). Units have a flat cost.
  cost_factor double precision not null default 2 check (cost_factor >= 1),
  -- Buildings only: energy required at level 1.
  base_energy integer not null default 0,
  -- Units only: FLEET or DEFENSE.
  unit_type text check (unit_type in ('FLEET', 'DEFENSE')),
  -- Units only: cargo capacity and combat stats.
  capacity bigint not null default 0 check (capacity >= 0),
  weapons double precision not null default 0 check (weapons >= 0),
  shield double precision not null default 0 check (shield >= 0),
  armor double precision not null default 0 check (armor >= 0)
);

-- Buildings/technologies that must reach a given level before an item is available.
create table item_requirements (
  id bigserial primary key,
  item_id bigint references item_definitions on delete cascade not null,
  required_item_id bigint references item_definitions not null,
  required_level integer not null check (required_level >= 1),
  unique (item_id, required_item_id)
);

create index item_requirements_item_id_idx on item_requirements (item_id);
