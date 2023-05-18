create table sequence (
  seq_id text not null,
  simulation_dataset_id int not null,
  metadata jsonb,

  created_at timestamptz not null default now(),

  constraint sequence_primary_key
    primary key (seq_id, simulation_dataset_id)
);
comment on table sequence is e''
  'A sequence product';
comment on column sequence.seq_id is e''
  'The FSW sequence specifier';
comment on column sequence.simulation_dataset_id is e''
  'The simulation dataset id whose outputs are associated with this sequence';
