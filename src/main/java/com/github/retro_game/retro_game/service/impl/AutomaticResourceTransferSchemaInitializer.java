package com.github.retro_game.retro_game.service.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class AutomaticResourceTransferSchemaInitializer {
  private final JdbcTemplate jdbcTemplate;

  AutomaticResourceTransferSchemaInitializer(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @PostConstruct
  void initialize() {
    jdbcTemplate.execute("""
        create table if not exists automatic_resource_transfers (
          id bigserial primary key,
          user_id bigint references users not null,
          source_body_id bigint references bodies on delete cascade not null,
          target_body_id bigint references bodies on delete cascade not null,
          enabled boolean not null,
          ship_kind int not null,
          ship_count int not null check (ship_count > 0),
          metal double precision not null check (metal >= 0),
          crystal double precision not null check (crystal >= 0),
          deuterium double precision not null check (deuterium >= 0),
          speed_factor int not null check (speed_factor between 1 and 10),
          run_hour int not null default 0 check (run_hour between 0 and 23),
          run_minute int not null default 0 check (run_minute between 0 and 59),
          next_run_at timestamptz not null,
          last_run_at timestamptz,
          last_error varchar(128),
          check (metal > 0 or crystal > 0 or deuterium > 0)
        )
        """);
    jdbcTemplate.execute("""
        create index if not exists automatic_resource_transfers_source_body_id_idx
          on automatic_resource_transfers (source_body_id)
        """);
    jdbcTemplate.execute("""
        create index if not exists automatic_resource_transfers_due_idx
          on automatic_resource_transfers (enabled, next_run_at, id)
        """);
    jdbcTemplate.execute("""
        alter table automatic_resource_transfers
          add column if not exists run_hour int not null default 0 check (run_hour between 0 and 23)
        """);
    jdbcTemplate.execute("""
        alter table automatic_resource_transfers
          add column if not exists run_minute int not null default 0 check (run_minute between 0 and 59)
        """);
    jdbcTemplate.execute("""
        do $$
        begin
          if exists (
            select 1
            from information_schema.columns
            where table_name = 'automatic_resource_transfers'
              and column_name = 'interval_minutes'
          ) then
            alter table automatic_resource_transfers alter column interval_minutes drop not null;
          end if;
        end $$;
        """);
  }
}
