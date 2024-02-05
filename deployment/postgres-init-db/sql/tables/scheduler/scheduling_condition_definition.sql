create table scheduling_condition_definition(
  condition_id integer not null,
  revision integer not null default 0,
  definition text not null,
  author text,
  created_at timestamptz not null default now(),

  constraint scheduling_condition_definition_pkey
    primary key (condition_id, revision),
  constraint scheduling_condition_definition_condition_exists
    foreign key (condition_id)
    references scheduling_condition_metadata
    on update cascade
    on delete cascade
);

comment on table scheduling_condition_definition is e''
  'The specific revisions of a scheduling condition''s definition';
comment on column scheduling_condition_definition.revision is e''
  'An identifier of this definition.';
comment on column scheduling_condition_definition.definition is e''
  'An executable expression in the Merlin scheduling language.';
comment on column scheduling_condition_definition.author is e''
  'The user who authored this revision.';
comment on column scheduling_condition_definition.created_at is e''
  'When this revision was created.';

create function scheduling_condition_definition_set_revision()
returns trigger
volatile
language plpgsql as $$
declare
  max_revision integer;
begin
  -- Grab the current max value of revision, or -1, if this is the first revision
  select coalesce((select revision
  from scheduling_condition_definition
  where condition_id = new.condition_id
  order by revision desc
  limit 1), -1)
  into max_revision;

  new.revision = max_revision + 1;
  return new;
end
$$;

create trigger scheduling_goal_definition_set_revision
  before insert on scheduling_condition_definition
  for each row
  execute function scheduling_condition_definition_set_revision();
