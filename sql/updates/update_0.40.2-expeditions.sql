do $$
declare
  constraint_record record;
begin
  for constraint_record in
    select conname
      from pg_constraint
     where conrelid = 'flights'::regclass
       and contype = 'c'
       and pg_get_constraintdef(oid) like '%target_position%'
  loop
    execute format('alter table flights drop constraint %I', constraint_record.conname);
  end loop;
  alter table flights add constraint flights_target_position_check check (target_position between 1 and 16);

  for constraint_record in
    select conname
      from pg_constraint
     where conrelid = 'other_reports'::regclass
       and contype = 'c'
       and pg_get_constraintdef(oid) like '%target_position%'
  loop
    execute format('alter table other_reports drop constraint %I', constraint_record.conname);
  end loop;
  alter table other_reports add constraint other_reports_target_position_check check (target_position between 1 and 16);
end
$$;
