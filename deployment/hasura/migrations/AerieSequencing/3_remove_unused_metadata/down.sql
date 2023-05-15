-- Command Dictionary
alter table command_dictionary
add column updated_at timestamptz not null default now();

create function command_dictionary_set_updated_at()
returns trigger
security definer
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create trigger trigger_command_dictionary_set_updated_at
  before
    update on command_dictionary
  for each row
  execute function command_dictionary_set_updated_at();

-- Sequence
alter table sequence
add column updated_at timestamptz not null default now(),
add column requested_by text not null default '';

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

create trigger sequence_set_updated_at_external_trigger
  after insert or update or delete on sequence_to_simulated_activity
  for each row
execute function sequence_set_updated_at_external();

call migrations.mark_migration_rolled_back('3');
