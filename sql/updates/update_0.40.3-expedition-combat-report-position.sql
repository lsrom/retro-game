do $$
declare
  constraint_record record;
begin
  for constraint_record in
    select conname
      from pg_constraint
     where conrelid = 'simplified_combat_reports'::regclass
       and contype = 'c'
       and pg_get_constraintdef(oid) like '%position%'
  loop
    execute format('alter table simplified_combat_reports drop constraint %I', constraint_record.conname);
  end loop;
  alter table simplified_combat_reports add constraint simplified_combat_reports_position_check
    check (position between 1 and 16);

  for constraint_record in
    select conname
      from pg_constraint
     where conrelid = 'debris_fields'::regclass
       and contype = 'c'
       and pg_get_constraintdef(oid) like '%position%'
  loop
    execute format('alter table debris_fields drop constraint %I', constraint_record.conname);
  end loop;
  alter table debris_fields add constraint debris_fields_position_check check (position between 1 and 16);
end
$$;
