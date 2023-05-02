create table expanded_sequences (
  id integer generated always as identity,

  expansion_run_id integer not null,
  seq_id text not null,
  expanded_sequence jsonb not null,
  
  created_at timestamptz not null default now(),

  constraint expanded_sequences_primary_key
    primary key (id),

  foreign key (expansion_run_id)
    references expansion_run (id)
    on delete cascade
);
