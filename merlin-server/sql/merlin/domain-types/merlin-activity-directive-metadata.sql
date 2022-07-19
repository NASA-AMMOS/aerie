create domain merlin_activity_directive_metadata_set as jsonb
  constraint merlin_activity_directive_metadata_set_is_object
    check(jsonb_typeof(value) = 'object');

comment on domain merlin_activity_directive_metadata_set is e''
  'The set of mission defined metadata associated with an activity directive.';

