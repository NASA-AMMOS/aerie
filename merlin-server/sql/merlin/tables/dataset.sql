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
