-- Drop Dependent Views
drop view rule_expansion_set_view;
drop view expansion_set_rule_view;

-- Sequence
comment on column sequence.requested_by is null;
alter table sequence
  drop column requested_by;

-- Expansion Set
drop trigger set_default_name on expansion_set;
drop function expansion_set_default_name;

comment on column expansion_set.updated_at is null;
comment on column expansion_set.created_at is null;
comment on column expansion_set.description is null;
comment on column expansion_set.updated_by is null;
comment on column expansion_set.owner is null;
comment on column expansion_set.name is null;

alter table expansion_set
  drop constraint expansion_set_unique_name_per_dict_and_model,
  drop column updated_by,
  drop column updated_at,
  drop column description,
  drop column owner,
  drop column name;

-- Expansion Rule
create or replace function expansion_rule_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

drop trigger set_default_name on expansion_rule;
drop function expansion_rule_default_name;

comment on column expansion_rule.updated_at is null;
comment on column expansion_rule.created_at is null;
comment on column expansion_rule.description is null;
comment on column expansion_rule.updated_by is null;
comment on column expansion_rule.owner is null;

alter table expansion_rule
  drop constraint expansion_rule_unique_name_per_dict_and_model,
  drop column description,
  drop column updated_by,
  drop column owner,
  drop column name;

-- Recreate Views
create view expansion_set_rule_view as
  select set_id, expansion_rule.*
  from expansion_set_to_rule left join expansion_rule
  on expansion_set_to_rule.rule_id = expansion_rule.id;

create view rule_expansion_set_view as
  select rule_id, expansion_set.*
  from expansion_set_to_rule left join expansion_set
  on expansion_set_to_rule.set_id = expansion_set.id;

call migrations.mark_migration_rolled_back('1');
