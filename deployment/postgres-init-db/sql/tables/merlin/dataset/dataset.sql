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
  language plpgsql as
$$begin
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

create function allocate_dataset_partitions(dataset_id integer)
  returns dataset
  security definer
  language plpgsql as $$
declare
  dataset_ref dataset;
begin
  select * from dataset where id = dataset_id into dataset_ref;
  if dataset_id is null
  then
    raise exception 'Cannot allocate partitions for non-existent dataset id %', dataset_id;
  end if;

  execute 'create table profile_segment_' || dataset_id || ' (
    like profile_segment including defaults including constraints
  );';
  execute 'alter table profile_segment
    attach partition profile_segment_' || dataset_id || ' for values in ('|| dataset_id ||');';

  execute 'create table event_' || dataset_id || ' (
      like event including defaults including constraints
    );';
  execute 'alter table event
    attach partition event_' || dataset_id || ' for values in (' || dataset_id || ');';

  execute 'create table span_' || dataset_id || ' (
       like span including defaults including constraints
    );';
  execute 'alter table span
    attach partition span_' || dataset_id || ' for values in (' || dataset_id || ');';

  -- Create a self-referencing foreign key on the span partition table. We avoid referring to the top level span table
  -- in order to avoid lock contention with concurrent inserts
  call span_add_foreign_key_to_partition('span_' || dataset_id);
  return dataset_ref;
end$$;

comment on function allocate_dataset_partitions is e''
  'Creates partition tables for the components of a dataset and attaches them to their partitioned tables.';

create function call_create_partition()
  returns trigger
  security invoker
  language plpgsql as $$ begin
    perform allocate_dataset_partitions(new.id);
return new;
end $$;

create trigger create_partition_on_simulation
  after insert on dataset
  for each row
  execute function call_create_partition();
