create or replace function constraint_check_constraint_run()
  returns trigger
  security definer
  language plpgsql as $$
begin
  update constraint_run
  set definition_outdated = true
  where constraint_id = new.id
    and constraint_definition != new.definition
    and definition_outdated = false;
  update constraint_run
  set definition_outdated = false
  where constraint_id = new.id
    and constraint_definition = new.definition
    and definition_outdated = true;
  return new;
end$$;

call migrations.mark_migration_applied('34');
