create table merlin.dataset (
  id integer generated always as identity,
  revision integer not null default 0,

  constraint dataset_synthetic_key
    primary key (id)
);

comment on table merlin.dataset is e''
  'A time-series dataset consisting of profiles and spans.'
'\n'
  'The actual data this dataset contains is stored in dedicated'
  'partitions of their corresponding tables.';
comment on column merlin.dataset.id is e''
  'The synthetic identifier for this dataset.';

create function merlin.delete_partitions()
returns trigger
security definer
language plpgsql as $$begin
  execute 'drop table if exists profile_segment_' || old.id || ' cascade';
  execute 'drop table if exists span_' || old.id || ' cascade';
  execute 'drop table if exists event_' || old.id || ' cascade';
return old;
end$$;

create trigger delete_partitions_trigger
  before delete on merlin.dataset
  for each row
  execute function merlin.delete_partitions();

create function merlin.delete_dataset_cascade()
  returns trigger
  security definer
  language plpgsql as
$$begin
  delete from merlin.span s where s.dataset_id = old.id;
  return old;
end$$;

create trigger delete_dataset_trigger
  after delete on merlin.dataset
  for each row
execute function merlin.delete_dataset_cascade();

comment on trigger delete_dataset_trigger on merlin.dataset is e''
  'Trigger to simulate an ON DELETE CASCADE foreign key constraint between span and dataset. The reason to'
  'implement this as a trigger is that this single trigger can cascade deletes to any partitions of span.'
  'If we used a foreign key, every new partition of span would need to add a new trigger to the dataset'
  'table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding new'
  'partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';

create function merlin.allocate_dataset_partitions(dataset_id integer)
  returns merlin.dataset
  security definer
  language plpgsql as $$
declare
  dataset_ref merlin.dataset;
begin
  select * from merlin.dataset d where d.id = dataset_id into dataset_ref;
  if dataset_id is null
  then
    raise exception 'Cannot allocate partitions for non-existent dataset id %', dataset_id;
  end if;

  execute 'create table merlin.profile_segment_' || dataset_id || ' (
    like merlin.profile_segment including defaults including constraints
  );';
  execute 'alter table merlin.profile_segment
    attach partition merlin.profile_segment_' || dataset_id || ' for values in ('|| dataset_id ||');';

  execute 'create table merlin.event_' || dataset_id || ' (
      like merlin.event including defaults including constraints
    );';
  execute 'alter table merlin.event
    attach partition merlin.event_' || dataset_id || ' for values in (' || dataset_id || ');';

  execute 'create table merlin.span_' || dataset_id || ' (
       like merlin.span including defaults including constraints
    );';
  execute 'alter table merlin.span
    attach partition merlin.span_' || dataset_id || ' for values in (' || dataset_id || ');';

  -- Create a self-referencing foreign key on the span partition table. We avoid referring to the top level span table
  -- in order to avoid lock contention with concurrent inserts
  call merlin.span_add_foreign_key_to_partition('merlin.span_' || dataset_id);
  return dataset_ref;
end$$;

comment on function merlin.allocate_dataset_partitions is e''
  'Creates partition tables for the components of a dataset and attaches them to their partitioned tables.';

create function merlin.call_create_partition()
  returns trigger
  security invoker
  language plpgsql as $$
begin
  perform merlin.allocate_dataset_partitions(new.id);
  return new;
end
$$;

create trigger create_partition_on_simulation
  after insert on merlin.dataset
  for each row
  execute function merlin.call_create_partition();
