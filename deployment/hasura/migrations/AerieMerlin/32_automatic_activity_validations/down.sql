drop trigger validation_entry_on_insert on activity_directive;

drop function activity_directive_validation_entry;

create or replace function activity_directive_set_arguments_updated_at()
  returns trigger
  security definer
  language plpgsql as $$begin
    call plan_locked_exception(new.plan_id);
    new.last_modified_arguments_at = now();
  return new;
end$$;

alter table activity_directive_validations
  drop column last_modified_arguments_at,
  drop column status,
  add column last_modified_at timestamptz not null default now();

comment on column activity_directive_validations.last_modified_at is e''
  'The time at which these argument validations were last modified.';

call migrations.mark_migration_rolled_back('32');
