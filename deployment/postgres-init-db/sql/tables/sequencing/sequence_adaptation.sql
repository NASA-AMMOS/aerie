create table sequencing.sequence_adaptation (
  id integer generated always as identity,
  adaptation text not null,
  created_at timestamptz not null default now(),
  owner text,
  updated_at timestamptz not null default now(),
  updated_by text,

  constraint sequence_adaptation_synthetic_key
    primary key (id),
  foreign key (owner)
    references permissions.users
    on update cascade
    on delete set null,
  foreign key (updated_by)
    references permissions.users
    on update cascade
    on delete set null
);

comment on table sequencing.sequence_adaptation is e''
  'A custom adaptation used to overwrite variable and linting rules for the sequence editor';
comment on column sequencing.sequence_adaptation.adaptation is e''
  'The sequencing adaptation code.';

create trigger set_timestamp
before update on sequencing.sequence_adaptation
for each row
execute function util_functions.set_updated_at();
