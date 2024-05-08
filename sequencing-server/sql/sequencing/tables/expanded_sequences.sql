create table expanded_sequences (
  id integer generated always as identity,

  expansion_run_id integer not null,
  seq_id text not null,
  simulation_dataset_id int not null,
  expanded_sequence jsonb not null,

  created_at timestamptz not null default now(),

  constraint expanded_sequences_primary_key
    primary key (id),

  constraint expanded_sequences_to_expansion_run_id
    foreign key (expansion_run_id)
      references expansion_run
      on delete cascade,

  constraint expanded_sequences_to_seq_id
    foreign key (seq_id, simulation_dataset_id)
      references sequence (seq_id, simulation_dataset_id)
      on delete cascade
);
