create table topic
(
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
      references dataset
      on update cascade
      on delete cascade
);

comment on table topic is e''
  'A representation of all topics that occurred at a single time point';
comment on column topic.dataset_id is e''
  'The dataset this topic is part of.';
comment on column topic.topic_index is e''
  'A unique number per simulation run that identifies this topic';
comment on column topic.value_schema is e''
  'The value schema describing the value of this topic';
comment on column topic.name is e''
  'The human readable name of this topic';
