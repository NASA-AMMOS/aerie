-- Drop Views
drop view expansion_set_rule_view;
drop view rule_expansion_set_view;

-- Expansion Rule
comment on column expansion_rule.updated_by is null;
comment on column expansion_rule.owner is null;

alter table expansion_rule
  drop column owner,
  drop column updated_by;
alter table expansion_rule
  add column owner text not null default '',
  add column updated_by text not null default '';

comment on column expansion_rule.owner is e''
  'The user responsible for this expansion rule.';
comment on column expansion_rule.updated_by is e''
  'The user who last updated this expansion rule.';

-- Expansion Set
comment on column expansion_set.owner is null;
comment on column expansion_set.updated_by is null;

alter table expansion_set
  drop column owner,
  drop column updated_by;
alter table expansion_set
  add column owner text not null default '',
  add column updated_by text not null default '';

comment on column expansion_set.owner is e''
  'The user responsible for the expansion set.';
comment on column expansion_set.updated_by is e''
  'The user who last updated this expansion set.';

-- User Sequence
comment on column user_sequence.owner is null;

alter table user_sequence
  drop column owner;
alter table user_sequence
  add column owner text not null default 'unknown';

comment on column user_sequence.owner is e''
  'Username of the user sequence owner.';

-- Fix Views
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

call migrations.mark_migration_rolled_back('6');
