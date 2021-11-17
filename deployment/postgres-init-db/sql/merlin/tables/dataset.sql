create table dataset (
  id integer generated always as identity,
  revision integer not null default 0,

  -- In the future, we might want to share datasets across multiple plans.
  -- In that case, the scoping plan_id and anchoring offset_from_plan_start should be moved into a new junction table
  -- identifying the planning contexts in which a dataset is available.
  plan_id integer null,
  offset_from_plan_start interval not null,

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

create or replace function create_partitions()
returns trigger
security definer
language plpgsql as $$begin
  execute
    'create table profile_segment_' || new.id
    || ' partition of profile_segment for values in (' || new.id || ')';
  execute
    'create table span_' || new.id
    || ' partition of span for values in (' || new.id || ')';
return new;
end$$;

create or replace function delete_partitions()
returns trigger
security definer
language plpgsql as $$begin
  execute 'drop table profile_segment_' || old.id || ' cascade';
  execute 'drop table span_' || old.id || ' cascade';
return old;
end$$;

do $$ begin
create trigger create_partitions_trigger
  after insert on dataset
  for each row
  execute function create_partitions();
exception
  when duplicate_object then null;
end $$;

do $$ begin
create trigger delete_partitions_trigger
  before delete on dataset
  for each row
  execute function delete_partitions();
exception
  when duplicate_object then null;
end $$;
