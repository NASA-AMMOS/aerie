alter table activity_directive_validations
  drop column last_modified_at,
  add column last_modified_arguments_at timestamptz not null;

comment on column activity_directive_validations.last_modified_arguments_at is e''
  'The time at which these argument validations were last modified.';

-- reuse exising  insert empty validations row on argument update
create or replace function activity_directive_set_arguments_updated_at()
  returns trigger
  security definer
  language plpgsql as
$$ begin
  call plan_locked_exception(new.plan_id);
  new.last_modified_arguments_at = now();

  -- clear old validations
  update activity_directive_validations
    set last_modified_arguments_at = new.last_modified_arguments_at,
        validations = '{}'
    where (directive_id, plan_id) = (new.id, new.plan_id);

  return new;
end $$;

create function activity_directive_validation_entry()
  returns trigger
  security definer
  language plpgsql as
$$ begin
  insert into activity_directive_validations
    (directive_id, plan_id, last_modified_arguments_at)
    values (new.id, new.plan_id, new.last_modified_arguments_at);
  return new;
end $$;

create trigger validation_entry_on_insert
  after insert on activity_directive
  for each row
execute function activity_directive_validation_entry();

call migrations.mark_migration_applied('32');
