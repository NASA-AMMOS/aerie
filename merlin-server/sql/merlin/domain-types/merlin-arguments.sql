create domain merlin_parameter_set as jsonb
  constraint merlin_parameter_set_is_object
    check(jsonb_typeof(value) = 'object');

comment on domain merlin_parameter_set is e''
  'A set of parameters accepted by a Merlin modeling entity, like an activity type or a mission model.';

create domain merlin_argument_set as jsonb
  constraint merlin_argument_set_is_object
    check(jsonb_typeof(value) = 'object');

comment on domain merlin_argument_set is e''
  'A set of arguments provided to a Merlin modeling entity, like an activity type or a mission model.';

create domain merlin_required_parameter_set as jsonb
  constraint merlin_required_parameter_set_is_array
    check(jsonb_typeof(value) = 'array');

comment on domain merlin_required_parameter_set is e''
  'A set of parameters required by a Merlin modeling entity, like an activity type or a mission model.';
