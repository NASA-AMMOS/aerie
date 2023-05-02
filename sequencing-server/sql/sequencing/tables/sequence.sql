create table sequence (
  seq_id text not null,
  simulation_dataset_id int not null,
  metadata jsonb,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  requested_by text not null default '',

  constraint sequence_primary_key
    primary key (seq_id, simulation_dataset_id)
);
comment on table sequence is e''
  'A sequence product';
comment on column sequence.seq_id is e''
  'The FSW sequence specifier';
comment on column sequence.simulation_dataset_id is e''
  'The simulation dataset id whose outputs are associated with this sequence';
comment on column sequence.metadata is e''
  'The metadata associated with this sequence';
comment on column sequence.requested_by is e''
  'The user who requested the expanded sequence.';

create or replace function sequence_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create or replace function sequence_set_updated_at_external()
  returns trigger
  security definer
  language plpgsql as $$begin
  update sequence
    set updated_at = now()
    where
        seq_id = old.seq_id and simulation_dataset_id = old.simulation_dataset_id
     or seq_id = new.seq_id and simulation_dataset_id = new.simulation_dataset_id;
  return null;
end$$;

create trigger set_timestamp
before update on sequence
for each row
execute function sequence_set_updated_at();
