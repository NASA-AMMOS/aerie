create table activity_instance_commands (
  activity_instance_id integer not null,
  commands jsonb not null,
  expansion_run_id integer not null,

  constraint activity_instance_primary_key
    primary key (activity_instance_id),

  foreign key (expansion_run_id)
  references expansion_run (id)
);

comment on table activity_instance_commands is e''
  'The commands generated from activities instances in the plan.';
comment on column activity_instance_commands.activity_instance_id is e''
  'The activity_instance in the plan.';
comment on column activity_instance_commands.commands is e''
  'Commands generated for the activity_instance.';
comment on column activity_instance_commands.expansion_run_id is e''
  'The configuration used during command generation';
