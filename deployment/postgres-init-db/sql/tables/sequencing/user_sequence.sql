create table sequencing.user_sequence (
  authoring_command_dict_id integer not null,
  created_at timestamptz not null default now(),
  definition text not null,
  id integer generated always as identity,
  name text not null,
  owner text,
  updated_at timestamptz not null default now(),

  constraint user_sequence_primary_key primary key (id)
);

comment on column sequencing.user_sequence.authoring_command_dict_id is e''
  'Command dictionary the user sequence was created with.';
comment on column sequencing.user_sequence.created_at is e''
  'Time the user sequence was created.';
comment on column sequencing.user_sequence.definition is e''
  'The user sequence definition string.';
comment on column sequencing.user_sequence.id is e''
  'ID of the user sequence.';
comment on column sequencing.user_sequence.name is e''
  'Human-readable name of the user sequence.';
comment on column sequencing.user_sequence.owner is e''
  'The user responsible for this sequence.';
comment on column sequencing.user_sequence.updated_at is e''
  'Time the user sequence was last updated.';

create trigger set_timestamp
before update on sequencing.user_sequence
for each row
execute function util_functions.set_updated_at();
