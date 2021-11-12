create table if not exists dataset (
  id integer generated always as identity,
  revision integer not null default 0,
  state text not null,
  reason text null,
  canceled boolean not null,

  -- In the future, we might want to share datasets across multiple plans.
  -- In that case, the scoping plan_id and anchoring offset_from_plan_start should be moved into a new junction table
  -- identifying the planning contexts in which a dataset is available.
  plan_id integer null,
  offset_from_plan_start interval not null,

  profile_segment_partition_table text not null,
  span_partition_table text not null,

  constraint dataset_synthetic_key
    primary key (id),
  constraint dataset_owned_by_plan
    foreign key (plan_id)
    references plan
    on update cascade
    on delete set null
);

comment on table dataset is e''
  'A time-series dataset consisting of profiles and traces.';
comment on column dataset.id is e''
  'The synthetic identifier for this dataset.';
comment on column dataset.plan_id is e''
  'The plan under which this dataset is scoped.';
comment on column dataset.offset_from_plan_start is e''
  'The time to judge dataset items against relative to the plan start.'
'\n'
  'If the dataset as a whole begins one day before the planning period begins, '
  'then this column should contain the interval ''1 day ago''.';
comment on column dataset.profile_segment_partition_table is e''
  'The name of the table partition containing profile segments for this dataset';

-- TODO: Add an ON DELETE trigger to drop the associated 'profile_segment' and 'span' partitions.
--   If that doesn't work, find some other way to clean up the dead partitions.
