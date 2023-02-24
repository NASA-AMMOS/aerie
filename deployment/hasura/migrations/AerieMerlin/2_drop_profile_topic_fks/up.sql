-- profile_segment
alter table profile_segment drop constraint profile_segment_owned_by_profile;

create function profile_segment_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(
    select from profile
      where profile.dataset_id = new.dataset_id
      and profile.id = new.profile_id
    for key share of profile)
    -- for key share is important: it makes sure that concurrent transactions cannot update
    -- the columns that compose the profile's key until after this transaction commits.
  then
    raise exception 'foreign key violation: there is no profile with id % in dataset %', new.profile_id, new.dataset_id;
  end if;
  return new;
end$$;

comment on function profile_segment_integrity_function is e''
  'Used to simulate a foreign key constraint between profile_segment and profile, to avoid acquiring a lock on the'
  'profile table when creating a new partition of profile_segment. This function checks that a corresponding profile'
  'exists for every inserted or updated profile_segment. A trigger that calls this function is added separately to each'
  'new partition of profile_segment.';

create constraint trigger insert_update_profile_segment_trigger
  after insert or update on profile_segment
  for each row
execute function profile_segment_integrity_function();

-- event
alter table event drop constraint event_owned_by_topic;

create function event_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(
    select from topic
      where topic.dataset_id = new.dataset_id
      and topic.topic_index = new.topic_index
    for key share of topic)
    -- for key share is important: it makes sure that concurrent transactions cannot update
    -- the columns that compose the topic's key until after this transaction commits.
  then
    raise exception 'foreign key violation: there is no topic with topic_index % in dataset %', new.topic_index, new.dataset_id;
  end if;
  return new;
end$$;

comment on function event_integrity_function is e''
  'Used to simulate a foreign key constraint between event and topic, to avoid acquiring a lock on the'
  'topic table when creating a new partition of event. This function checks that a corresponding topic'
  'exists for every inserted or updated event. A trigger that calls this function is added separately to each'
  'new partition of event.';

create constraint trigger insert_update_event_trigger
  after insert or update on event
  for each row
execute function event_integrity_function();

-- profile
create function delete_profile_cascade()
  returns trigger
  security invoker
  language plpgsql as $$begin
  delete from profile_segment
  where profile_segment.dataset_id = old.dataset_id and profile_segment.profile_id = old.id;
  return old;
end$$;

create trigger delete_profile_trigger
  after delete on profile
  for each row
execute function delete_profile_cascade();

create function update_profile_cascade()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if old.id != new.id or old.dataset_id != new.dataset_id
  then
    update profile_segment
    set profile_id = new.id,
        dataset_id = new.dataset_id
    where profile_segment.dataset_id = old.dataset_id and profile_segment.profile_id = old.id;
  end if;
  return new;
end$$;

comment on trigger delete_profile_trigger on profile is e''
  'Trigger to simulate an ON DELETE CASCADE foreign key constraint between profile_segment and profile. The reason to'
  'implement this as a trigger is that this single trigger can cascade deletes to any partitions of profile_segment.'
  'If we used a foreign key, every new partition of profile_segment would need to add a new cascade delete trigger to'
  'the profile table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding'
  'new partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';

create trigger update_profile_trigger
  after update on profile
  for each row
execute function update_profile_cascade();

comment on trigger update_profile_trigger on profile is e''
  'Trigger to simulate an ON UPDATE CASCADE foreign key constraint between profile_segment and profile. The reason to'
  'implement this as a trigger is that this single trigger can propagate updates to any partitions of profile_segment.'
  'If we used a foreign key, every new partition of profile_segment would need to add a new trigger to the profile'
  'table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding new'
  'partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';

-- topic
create function delete_topic_cascade()
  returns trigger
  security invoker
  language plpgsql as $$begin
  delete from event
  where event.topic_index = old.topic_index and event.dataset_id = old.dataset_id;
  return old;
end$$;

create trigger delete_topic_trigger
  after delete on topic
  for each row
execute function delete_topic_cascade();

comment on trigger delete_topic_trigger on topic is e''
  'Trigger to simulate an ON DELETE CASCADE foreign key constraint between event and topic. The reason to'
  'implement this as a trigger is that this single trigger can cascade deletes to any partitions of event.'
  'If we used a foreign key, every new partition of event would need to add a new trigger to the topic'
  'table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding new'
  'partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';

create function update_topic_cascade()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if old.topic_index != new.topic_index or old.dataset_id != new.dataset_id
  then
    update event
    set topic_index = new.topic_index,
        dataset_id = new.dataset_id
    where event.dataset_id = old.dataset_id and event.topic_index = old.topic_index;
  end if;
  return new;
end$$;

create trigger update_topic_trigger
  after update on topic
  for each row
execute function update_topic_cascade();

comment on trigger update_topic_trigger on topic is e''
  'Trigger to simulate an ON UPDATE CASCADE foreign key constraint between event and topic. The reason to'
  'implement this as a trigger is that this single trigger can propagate updates to any partitions of event.'
  'If we used a foreign key, every new partition of event would need to add a new trigger to the topic'
  'table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding new'
  'partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';

-- span
alter table span drop constraint span_has_parent_span;
alter table span drop constraint span_owned_by_dataset;

create function span_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(select from dataset where dataset.id = new.dataset_id for key share of dataset)
  then
    raise exception 'foreign key violation: there is no dataset with id %', new.dataset_id;
  end if;
  return new;
end$$;

comment on function span_integrity_function is e''
  'Used to simulate a foreign key constraint between span and dataset, to avoid acquiring a lock on the'
  'dataset table when creating a new partition of span. This function checks that a corresponding dataset'
  'exists for every inserted or updated span. A trigger that calls this function is added separately to each'
  'new partition of span.';

create constraint trigger insert_update_span_trigger
  after insert or update on span
  for each row
execute function span_integrity_function();

create procedure span_add_foreign_key_to_partition(table_name varchar)
  security invoker
  language plpgsql as $$begin
  execute 'alter table ' || table_name || ' add constraint span_has_parent_span
    foreign key (dataset_id, parent_id)
    references ' || table_name || '
    on update cascade
    on delete cascade;';
end$$;

comment on procedure span_add_foreign_key_to_partition is e''
  'Creates a self-referencing foreign key on a particular partition of the span table. This should be called'
  'on every partition as soon as it is created';

do $$
  declare
    dataset_id integer;
  begin
    for dataset_id in select id from dataset
      loop
        if exists(select from pg_tables where schemaname = 'public' and tablename = 'span_' || dataset_id) then
          call span_add_foreign_key_to_partition('span_' || dataset_id);
        end if;
      end loop;
  end
$$;

-- dataset
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

call migrations.mark_migration_applied('2');
