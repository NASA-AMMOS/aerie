create table merlin.topic (
  dataset_id integer not null,
  topic_index integer not null,

  name text,
  value_schema jsonb,

  -- It would make sense for name to be part of the topic's key. This requires enforcing that topics
  -- have unique names per simulation. Since, as of writing, this is not the case, topics instead use
  -- an integer topic_index.
  constraint topic_natural_key
    primary key (dataset_id, topic_index),

  -- TODO: Use the natural key when we can be confident that topic names are unique for a given dataset_id.
  -- constraint topic_natural_key
  --   primary key (dataset_id, name),
  constraint topic_owned_by_dataset
    foreign key (dataset_id)
      references merlin.dataset
      on update cascade
      on delete cascade
);

comment on table merlin.topic is e''
  'A representation of all topics that occurred at a single time point';
comment on column merlin.topic.dataset_id is e''
  'The dataset this topic is part of.';
comment on column merlin.topic.topic_index is e''
  'A unique number per simulation run that identifies this topic';
comment on column merlin.topic.value_schema is e''
  'The value schema describing the value of this topic';
comment on column merlin.topic.name is e''
  'The human readable name of this topic';

create function merlin.delete_topic_cascade()
  returns trigger
  security invoker
  language plpgsql as $$
begin
  delete from merlin.event e
  where e.topic_index = old.topic_index and e.dataset_id = old.dataset_id;
  return old;
end
$$;

create trigger delete_topic_trigger
  after delete on merlin.topic
  for each row
execute function merlin.delete_topic_cascade();

comment on trigger delete_topic_trigger on merlin.topic is e''
  'Trigger to simulate an ON DELETE CASCADE foreign key constraint between event and topic. The reason to'
  'implement this as a trigger is that this single trigger can cascade deletes to any partitions of event.'
  'If we used a foreign key, every new partition of event would need to add a new trigger to the topic'
  'table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding new'
  'partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';

create function merlin.update_topic_cascade()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if old.topic_index != new.topic_index or old.dataset_id != new.dataset_id
  then
    update merlin.event e
    set topic_index = new.topic_index,
        dataset_id = new.dataset_id
    where e.dataset_id = old.dataset_id and e.topic_index = old.topic_index;
  end if;
  return new;
end$$;

create trigger update_topic_trigger
  after update on merlin.topic
  for each row
execute function merlin.update_topic_cascade();

comment on trigger update_topic_trigger on merlin.topic is e''
  'Trigger to simulate an ON UPDATE CASCADE foreign key constraint between event and topic. The reason to'
  'implement this as a trigger is that this single trigger can propagate updates to any partitions of event.'
  'If we used a foreign key, every new partition of event would need to add a new trigger to the topic'
  'table - which requires acquiring a lock that conflicts with concurrent inserts. In order to allow adding new'
  'partitions concurrently with inserts to referenced tables, we have chosen to forego foreign keys from partitions'
  'to other tables in favor of these hand-written triggers';
