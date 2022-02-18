create table plan_dataset (
  plan_id integer not null,
  dataset_id integer not null,

  offset_from_plan_start interval not null,

  constraint plan_dataset_primary_key
    primary key (plan_id, dataset_id),
  constraint plan_dataset_references_plan
    foreign key (plan_id)
    references plan
    on update cascade
    on delete cascade,
  constraint plan_dataset_references_dataset
    foreign key (dataset_id)
    references dataset
    on update cascade
    on delete cascade
);

comment on column plan_dataset.plan_id is e''
  'The ID of the plan to which the dataset is associated.';
comment on column plan_dataset.dataset_id is e''
  'The ID of the dataset associated with the plan.';
comment on column plan_dataset.offset_from_plan_start is e''
  'The time to judge dataset items against relative to the plan start.'
'\n'
  'If the dataset as a whole begins one day before the planning period begins, '
  'then this column should contain the interval ''1 day ago''.';

create or replace function create_dataset()
returns trigger
security definer
language plpgsql as $$begin
  insert into dataset
  default values
  returning id into new.dataset_id;
return new;
end$$;

-- To calculate this offset, we are going to grab any existing plan dataset with
-- the same associated dataset, add the offset to the plan start time to find the
-- start time of the dataset, and then subtract out the NEW plan start time to
-- determine the offset in the NEW plan dataset
create or replace function calculate_offset()
returns trigger
security definer
language plpgsql as $$
declare
  reference plan_dataset;
  reference_plan_start timestamptz;
  dataset_start timestamptz;
  new_plan_start timestamptz;
begin
  -- Get an existing association with this dataset for reference
  select into reference * from plan_dataset
  where dataset_id = new.dataset_id;

  -- If no reference exists, raise an exception
  if reference is null
  then
    raise exception 'Nonexistent dataset_id --> %', new.dataset_id
          using hint = 'dataset_id must already be associated with a plan.';
  end if;

  -- Get the plan start times
  select start_time into reference_plan_start from plan where id = reference.plan_id;
  select start_time into new_plan_start from plan where id = new.plan_id;

  -- calculate and assign the new offset from plan start
  dataset_start := reference_plan_start + reference.offset_from_plan_start;
  new.offset_from_plan_start = dataset_start - new_plan_start;
return new;
end$$;

create or replace function process_delete()
returns trigger
security definer
language plpgsql as $$begin
  if (select count(*) from plan_dataset where dataset_id = old.dataset_id) = 0
  then
    delete from dataset
    where id = old.dataset_id;
  end if;
return old;
end$$;

-- If a new row is created with no dataset in mind, create the dataset
do $$ begin
create trigger create_dataset_trigger
  before insert on plan_dataset
  for each row
  when (new.dataset_id is null)
  execute function create_dataset();
exception
  when duplicate_object then null;
end $$;

-- If a new row is created for an existing dataset, calculate the offset
do $$ begin
create trigger calculate_offset_trigger
  before insert on plan_dataset
  for each row
  when (new.dataset_id is not null)
  execute function calculate_offset();
exception
  when duplicate_object then null;
end $$;

-- When a row is deleted, check if any rows for the dataset remain
-- If not, delete the dataset
do $$ begin
create trigger delete_dataset_trigger
  after delete on plan_dataset
  for each row
  execute function process_delete();
exception
  when duplicate_object then null;
end $$;
