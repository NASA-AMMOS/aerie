create table span (
  id integer generated always as identity,

  dataset_id integer not null,
  parent_id integer null,

  start_offset interval not null,
  duration interval null,
  type text not null,
  attributes jsonb not null,

  constraint span_synthetic_key
    primary key (dataset_id, id),
  constraint span_owned_by_dataset
    foreign key (dataset_id)
    references dataset
    on update cascade
    on delete cascade,
  constraint span_has_parent_span
    foreign key (dataset_id, parent_id)
    references span
    on update cascade
    on delete cascade
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
