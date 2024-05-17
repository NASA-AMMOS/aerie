begin;
-- Move the contents of "public" to "sequencing"
alter schema public rename to sequencing;
comment on schema sequencing is 'Sequencing Service Schema';
create schema public;

-- Move Tags Table
alter table metadata.expansion_rule_tags set schema tags;
-- Metadata Schema is empty now
drop schema metadata;

-- Update Triggers
drop trigger set_timestamp on sequencing.expansion_rule;
drop function sequencing.expansion_rule_set_updated_at();

create trigger set_timestamp
before update on sequencing.expansion_rule
for each row
execute function util_functions.set_updated_at();

drop trigger set_timestamp on sequencing.user_sequence;
drop function sequencing.user_sequence_set_updated_at();

create trigger set_timestamp
before update on sequencing.user_sequence
for each row
execute function util_functions.set_updated_at();

-- Update Foreign Keys, handling orphans first
delete from tags.expansion_rule_tags
  where not exists(
   select from tags.tags t
   where tag_id = t.id);
alter table tags.expansion_rule_tags
  add foreign key (tag_id) references tags.tags
  on update cascade
  on delete cascade;

delete from sequencing.expanded_sequences
  where not exists(
   select from merlin.simulation_dataset sd
   where simulation_dataset_id = sd.id);
alter table sequencing.expanded_sequences
  add constraint expanded_sequences_to_sim_run
    foreign key (simulation_dataset_id)
      references merlin.simulation_dataset
      on delete cascade;

update sequencing.expansion_rule
  set authoring_mission_model_id = null
  where not exists(
   select from merlin.mission_model m
   where authoring_mission_model_id = m.id);
update sequencing.expansion_rule
  set owner = null
  where not exists(
   select from permissions.users u
   where owner = u.username);
update sequencing.expansion_rule
  set updated_by = null
  where not exists(
   select from permissions.users u
   where updated_by = u.username);
alter table sequencing.expansion_rule
  add foreign key (authoring_mission_model_id)
    references merlin.mission_model
    on update cascade
    on delete set null,
  add foreign key (owner)
    references permissions.users
    on update cascade
    on delete set null,
  add foreign key (updated_by)
    references permissions.users
    on update cascade
    on delete set null;
comment on column sequencing.expansion_rule.activity_type is e''
  'The activity type this expansion rule applies to. This type is not model-specific.';

delete from sequencing.expansion_run
  where not exists(
   select from merlin.simulation_dataset sd
   where simulation_dataset_id = sd.id);
alter table sequencing.expansion_run
  add foreign key (simulation_dataset_id)
    references merlin.simulation_dataset
    on delete cascade;

delete from sequencing.expansion_set
  where not exists(
   select from merlin.mission_model m
   where mission_model_id = m.id);
update sequencing.expansion_set
  set owner = null
  where not exists(
   select from permissions.users u
   where owner = u.username);
update sequencing.expansion_set
  set updated_by = null
  where not exists(
   select from permissions.users u
   where updated_by = u.username);
alter table sequencing.expansion_set
  add foreign key (mission_model_id)
    references merlin.mission_model
    on delete cascade,
  add foreign key (owner)
    references permissions.users
    on update cascade
    on delete set null,
  add foreign key (updated_by)
    references permissions.users
    on update cascade
    on delete set null;

delete from sequencing.sequence
  where not exists(
   select from merlin.simulation_dataset sd
   where simulation_dataset_id = sd.id);
alter table sequencing.sequence
  add foreign key (simulation_dataset_id)
    references merlin.simulation_dataset
    on delete cascade;

delete from sequencing.sequence_to_simulated_activity
  where not exists(
   select from merlin.simulation_dataset sd
   where simulation_dataset_id = sd.id);
alter table sequencing.sequence_to_simulated_activity
  add constraint sequence_to_sim_run
      foreign key (simulation_dataset_id)
      references merlin.simulation_dataset
      on delete cascade;

delete from sequencing.user_sequence
  where not exists(
   select from sequencing.command_dictionary cd
   where authoring_command_dict_id = cd.id);
update sequencing.user_sequence
  set owner = null
  where not exists(
   select from permissions.users u
   where owner = u.username);
alter table sequencing.user_sequence
  add foreign key (authoring_command_dict_id)
    references sequencing.command_dictionary
    on delete cascade,
  add foreign key (owner)
    references permissions.users
    on update cascade
    on delete cascade;

-- Update Views
create or replace view sequencing.expansion_set_rule_view as
select str.set_id,
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
from sequencing.expansion_set_to_rule str left join sequencing.expansion_rule rule
  on str.rule_id = rule.id;
create or replace view sequencing.rule_expansion_set_view as
select str.rule_id,
			 set.id,
			 set.name,
			 set.owner,
			 set.description,
			 set.command_dict_id,
			 set.mission_model_id,
			 set.created_at,
			 set.updated_at,
			 set.updated_by
from sequencing.expansion_set_to_rule str left join sequencing.expansion_set set
  on str.set_id = set.id;

end;
