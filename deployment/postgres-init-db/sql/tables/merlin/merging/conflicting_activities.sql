-- Stores a list of all activities in conflict in a merge
create table merlin.conflicting_activities(
  merge_request_id integer
    references merlin.merge_request
    on update cascade
    on delete cascade,
  activity_id integer,

  primary key (activity_id, merge_request_id),

  change_type_supplying merlin.activity_change_type not null
    check ( change_type_supplying = 'delete' or change_type_supplying = 'modify' ),
  change_type_receiving merlin.activity_change_type not null
    check ( change_type_receiving = 'delete' or change_type_receiving = 'modify' ),
  resolution merlin.conflict_resolution default 'none'
);

comment on table merlin.conflicting_activities is e''
  'An activity directive in an in-progress merge '
  'where the supplying, receiving, and merge base versions of this activity directive are all different.';
comment on column merlin.conflicting_activities.merge_request_id is e''
  'The merge request associated with this conflicting activity.\n'
  'Half of the natural key associated with this table, alongside activity_id.';
comment on column merlin.conflicting_activities.activity_id is e''
  'The activity directive that is in conflict.\n'
  'Half of the natural key associated with this table, alongside merge_request_id.';
comment on column merlin.conflicting_activities.change_type_supplying is e''
  'The type of change that has occurred between the merge base and the version of this activity'
  ' in the supplying plan.\n'
  'Must be either "delete" or "modify".';
comment on column merlin.conflicting_activities.change_type_receiving is e''
  'The type of change that has occurred between the merge base and the version of this activity'
  ' in the receiving plan.\n'
  'Must be either "delete" or "modify".';
comment on column merlin.conflicting_activities.resolution is e''
  'The version of this activity to be used when committing this merge.\n'
  'Can be either "none", "receiving" or "supplying".';
