create table merlin.event (
  dataset_id integer not null,
  real_time interval not null,
  transaction_index integer not null,
  causal_time text,

  value jsonb,
  topic_index integer not null,
  span_id integer,

  constraint event_natural_key
    primary key (dataset_id, real_time, transaction_index, causal_time)
)
partition by list (dataset_id);

comment on table merlin.event is e''
  'A representation of all events that occurred at a single time point';
comment on column merlin.event.dataset_id is e''
  'The dataset this event is part of.';
comment on column merlin.event.real_time is e''
  'The simulation time at which this event takes place';
comment on column merlin.event.transaction_index is e''
  'When multiple transactions occur at the same real_time, the transaction index will disambiguate them';
comment on column merlin.event.causal_time is e''
  'A string that allows any two events at the same real time to be compared for causal relationships.';
comment on column merlin.event.value is e''
  'The value of this event as a json blob';
comment on column merlin.event.topic_index is e''
  'The topic of this event';
comment on column merlin.event.span_id is e''
  'The span of this event';

create function merlin.event_integrity_function()
  returns trigger
  security invoker
  language plpgsql as $$begin
  if not exists(
    select from merlin.topic t
      where t.dataset_id = new.dataset_id
      and t.topic_index = new.topic_index
    for key share of t)
    -- for key share is important: it makes sure that concurrent transactions cannot update
    -- the columns that compose the topic's key until after this transaction commits.
  then
    raise exception 'foreign key violation: there is no topic with topic_index % in dataset %', new.topic_index, new.dataset_id;
  end if;
  return new;
end$$;

comment on function merlin.event_integrity_function is e''
  'Used to simulate a foreign key constraint between event and topic, to avoid acquiring a lock on the'
  'topic table when creating a new partition of event. This function checks that a corresponding topic'
  'exists for every inserted or updated event. A trigger that calls this function is added separately to each'
  'new partition of event.';

create constraint trigger insert_update_event_trigger
  after insert or update on merlin.event
  for each row
execute function merlin.event_integrity_function();
