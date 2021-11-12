do $$ begin
  create domain merlin_parameter_set as jsonb
    constraint merlin_parameter_set_is_object
      check(jsonb_typeof(value) = 'object');
exception
  when duplicate_object then null;
end $$;

comment on domain merlin_parameter_set is e''
  'A set of parameters accepted by a Merlin modeling entity, like an activity type or a mission model.';

do $$ begin
  create domain merlin_argument_set as jsonb
    constraint merlin_argument_set_is_object
      check(jsonb_typeof(value) = 'object');
exception
  when duplicate_object then null;
end $$;

comment on domain merlin_argument_set is e''
  'A set of arguments provided to a Merlin modeling entity, like an activity type or a mission model.';
