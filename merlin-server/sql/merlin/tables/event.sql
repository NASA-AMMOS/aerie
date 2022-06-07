create table event
(
  dataset_id integer not null,
  real_time interval not null,
  transaction_index integer not null,
  causal_time text,

  value jsonb,
  topic_index integer not null,

  constraint event_natural_key
    primary key (dataset_id, real_time, transaction_index, causal_time),
  constraint event_owned_by_topic
    foreign key (dataset_id, topic_index)
      references topic
      on update cascade
      on delete cascade
)
partition by list (dataset_id);

comment on table event is e''
  'A representation of all events that occurred at a single time point';
comment on column event.dataset_id is e''
  'The dataset this event is part of.';
comment on column event.real_time is e''
  'The simulation time at which this event takes place';
comment on column event.transaction_index is e''
  'When multiple transactions occur at the same real_time, the transaction index will disambiguate them';
comment on column event.causal_time is e''
  'A string that allows any two events at the same real time to be compared for causal relationships.';
comment on column event.value is e''
  'The value of this event as a json blob';
comment on column event.topic_index is e''
  'The topic of this event';
