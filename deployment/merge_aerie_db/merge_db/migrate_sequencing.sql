begin;
-- Move the contents of "public" to "sequencing"
alter schema public rename to sequencing;
comment on schema sequencing is 'Sequencing Service Schema';
create schema public;

-- Move Tags Table
alter table metadata.expansion_rule_tags set schema tags;
-- Metadata Schema is empty now
drop schema metadata;

-- Update Foreign Keys
alter table tags.expansion_rule_tags
  add foreign key (tag_id) references tags.tags
  on update cascade
  on delete cascade;
alter table sequencing.expanded_sequences
  add constraint expanded_sequences_to_sim_run
    foreign key (simulation_dataset_id)
      references merlin.simulation_dataset
      on delete cascade;
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

alter table sequencing.expansion_run
  add foreign key (simulation_dataset_id)
    references merlin.simulation_dataset
    on delete cascade;
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
alter table sequencing.sequence
  add foreign key (simulation_dataset_id)
    references merlin.simulation_dataset
    on delete cascade;
alter table sequencing.sequence_to_simulated_activity
  add constraint sequence_to_sim_run
      foreign key (simulation_dataset_id)
      references merlin.simulation_dataset
      on delete cascade;
alter table sequencing.user_sequence
  add foreign key (authoring_command_dict_id)
    references sequencing.command_dictionary
    on delete cascade,
  add foreign key (owner)
    references permissions.users
    on update cascade
    on delete cascade;

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
