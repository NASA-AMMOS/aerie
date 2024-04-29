create table scheduling_specification_conditions (
  specification_id integer not null,
  condition_id integer not null,
  condition_revision integer, -- latest is NULL
  enabled boolean default true,

  constraint scheduling_specification_conditions_primary_key
    primary key (specification_id, condition_id),
  constraint scheduling_specification_conditions_specification_exists
    foreign key (specification_id)
      references scheduling_specification
      on update cascade
      on delete cascade,
  constraint scheduling_specification_condition_exists
    foreign key (condition_id)
      references scheduling_condition_metadata
      on update cascade
      on delete restrict,
  constraint scheduling_specification_condition_definition_exists
    foreign key (condition_id, condition_revision)
      references scheduling_condition_definition
      on update cascade
      on delete restrict
);

comment on table scheduling_specification_conditions is e''
  'The set of scheduling conditions to be used on a given plan.';
comment on column scheduling_specification_conditions.specification_id is e''
  'The plan scheduling specification which this condition is on. Half of the primary key.';
comment on column scheduling_specification_conditions.condition_id is e''
  'The ID of a specific condition in the specification. Half of the primary key.';
comment on column scheduling_specification_conditions.condition_revision is e''
  'The version of the condition definition to use. Leave NULL to use the latest version.';
comment on column scheduling_specification_conditions.enabled is e''
  'Whether to use a given condition. Defaults to TRUE.';

create function increment_spec_revision_on_conditions_spec_update()
  returns trigger
  security definer
language plpgsql as $$
begin
  update scheduling_specification
  set revision = revision + 1
  where id = new.specification_id;
  return new;
end;
$$;

create trigger increment_revision_on_condition_update
  before insert or update on scheduling_specification_conditions
  for each row
  execute function increment_spec_revision_on_conditions_spec_update();

create function increment_spec_revision_on_conditions_spec_delete()
  returns trigger
  security definer
language plpgsql as $$
begin
  update scheduling_specification
  set revision = revision + 1
  where id = new.specification_id;
  return new;
end;
$$;

create trigger increment_revision_on_condition_delete
  before delete on scheduling_specification_conditions
  for each row
  execute function increment_spec_revision_on_conditions_spec_delete();
