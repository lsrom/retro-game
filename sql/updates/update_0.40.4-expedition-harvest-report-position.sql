do $$
declare
  constraint_record record;
begin
  for constraint_record in
    select conname
      from pg_constraint
     where conrelid = 'harvest_reports'::regclass
       and contype = 'c'
       and pg_get_constraintdef(oid) like '%position%'
  loop
    execute format('alter table harvest_reports drop constraint %I', constraint_record.conname);
  end loop;
  alter table harvest_reports add constraint harvest_reports_position_check check (position between 1 and 16);
end
$$;
