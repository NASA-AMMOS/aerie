create table span (
  id integer generated always as identity,

  dataset_id integer not null,
  parent_id integer null,

  start_offset interval not null,
  duration interval null,
  type text not null,
  attributes jsonb not null,

  constraint span_synthetic_key
    primary key (dataset_id, id)
)
partition by list (dataset_id);

comment on table span is e''
  'A temporal window of interest. A span may be refined by its children, providing additional information over '
  'more specific windows.';

comment on column span.id is e''
  'The synthetic identifier for this span.';
comment on column span.dataset_id is e''
  'The dataset this span is part of.';
comment on column span.parent_id is e''
  'The span this span refines.';
comment on column span.start_offset is e''
  'The offset from the dataset start at which this span begins.';
comment on column span.duration is e''
  'The amount of time this span extends for.';
comment on column span.type is e''
  'The type of span, implying the shape of its attributes.';
comment on column span.attributes is e''
  'A set of named values annotating this span as a whole.';

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
