-- Drop Dependent Views
drop view rule_expansion_set_view;
drop view expansion_set_rule_view;

-- Expansion Rule
alter table expansion_rule
  add column name text null,
  add column owner text not null default '',
  add column updated_by text not null default '',
  add column description text not null default '',
  add constraint expansion_rule_unique_name_per_dict_and_model
    unique (authoring_mission_model_id, authoring_command_dict_id, name);

update expansion_rule
set name = id
where name is null;

alter table expansion_rule
  alter column name set not null;

create function expansion_rule_default_name()
returns trigger
security invoker
language plpgsql as $$begin
  new.name = new.id;
  return new;
end
$$;

create trigger set_default_name
before insert on expansion_rule
for each row
when ( new.name is null )
execute function expansion_rule_default_name();

comment on column expansion_rule.owner is e''
  'The user responsible for this expansion rule.';
comment on column expansion_rule.updated_by is e''
  'The user who last updated this expansion rule.';
comment on column expansion_rule.description is e''
  'A description of this expansion rule.';
comment on column expansion_rule.created_at is e''
  'The time this expansion rule was created';
comment on column expansion_rule.updated_at is e''
  'The time this expansion rule was last updated.';

create or replace function expansion_rule_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  update expansion_set es
    set updated_at = new.updated_at
    from expansion_set_to_rule esr
    where esr.rule_id = new.id
      and esr.set_id = es.id;
  return new;
end$$;

-- Expansion Set
alter table expansion_set
  add column name text null,
  add column owner text not null default '',
  add column description text not null default '',
  add column updated_at timestamptz not null default now(),
  add column updated_by text not null default '',
  add constraint expansion_set_unique_name_per_dict_and_model
    unique (mission_model_id, command_dict_id, name);
comment on column expansion_set.name is e''
  'The human-readable name of the expansion set.';
comment on column expansion_set.owner is e''
  'The user responsible for the expansion set.';
comment on column expansion_set.updated_by is e''
  'The user who last updated this expansion set.';
comment on column expansion_set.description is e''
  'A description of this expansion set.';
comment on column expansion_set.created_at is e''
  'The time this expansion set was created';
comment on column expansion_set.updated_at is e''
  'The time this expansion set or one of its expansion rules was last updated.';

update expansion_set
set name = id
where name is null;

alter table expansion_set
  alter column name set not null;

create function expansion_set_default_name()
returns trigger
security invoker
language plpgsql as $$begin
  new.name = new.id;
  return new;
end
$$;

create trigger set_default_name
before insert on expansion_set
for each row
when ( new.name is null )
execute function expansion_set_default_name();

-- Sequence
alter table sequence
  add column requested_by text not null default '';
comment on column sequence.requested_by is e''
  'The user who requested the expanded sequence.';

-- Recreate Views
create view expansion_set_rule_view as
select expansion_set_to_rule.set_id,
			 rule.id,
			 rule.activity_type,
			 rule.expansion_logic,
			 rule.authoring_command_dict_id,
			 rule.authoring_mission_model_id,
			 rule.created_at,
			 rule.updated_at,
			 rule.name,
			 rule.owner,
			 rule.updated_by,
			 rule.description
from expansion_set_to_rule left join expansion_rule rule
  on expansion_set_to_rule.rule_id = rule.id;

create view rule_expansion_set_view as
select expansion_set_to_rule.rule_id,
			 set.id,
			 set.name,
			 set.owner,
       set.description,
			 set.command_dict_id,
			 set.mission_model_id,
			 set.created_at,
			 set.updated_at,
			 set.updated_by
from expansion_set_to_rule left join expansion_set set
  on expansion_set_to_rule.set_id = set.id;

call migrations.mark_migration_applied('1');
