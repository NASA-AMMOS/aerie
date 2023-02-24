create table dataset (
  id integer generated always as identity,
  revision integer not null default 0,

  constraint dataset_synthetic_key
    primary key (id)
);

comment on table dataset is e''
  'A time-series dataset consisting of profiles and spans.'
'\n'
  'The actual data this dataset contains is stored in dedicated'
  'partitions of their corresponding tables.';
comment on column dataset.id is e''
  'The synthetic identifier for this dataset.';

create or replace function delete_partitions()
returns trigger
security definer
language plpgsql as $$begin
  execute 'drop table if exists profile_segment_' || old.id || ' cascade';
  execute 'drop table if exists span_' || old.id || ' cascade';
  execute 'drop table if exists event_' || old.id || ' cascade';
return old;
end$$;

do $$ begin
create trigger delete_partitions_trigger
  before delete on dataset
  for each row
  execute function delete_partitions();
exception
  when duplicate_object then null;
end $$;

create function delete_dataset_cascade()
  returns trigger
  security definer
  language plpgsql as $$begin
    delete from span where span.dataset_id = old.id;
    return old;
end$$;

create trigger delete_dataset_trigger
  after delete on dataset
  for each row
  execute function delete_dataset_cascade();

comment on trigger delete_dataset_trigger on dataset is e''
  'Trigger to simulate an ON DELETE CASCADE foreign key constraint between span and dataset. The reason to'
  'implement this as a trigger is that this single trigger can cascade deletes to any partitions of span.'
  'If we used a foreign key, every new partition of span would need to add a new trigger to the dataset'
  'table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding new'
  'partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';
