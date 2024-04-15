create table scheduler.scheduling_goal_metadata (
  id integer generated always as identity,

  name text not null,
  description text not null default '',
  public boolean not null default false,

  owner text,
  updated_by text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint scheduling_goal_metadata_pkey
    primary key (id),
  constraint goal_owner_exists
    foreign key (owner)
    references permissions.users
    on update cascade
    on delete set null,
  constraint goal_updated_by_exists
    foreign key (updated_by)
    references permissions.users
    on update cascade
    on delete set null
);

-- A partial index is used to enforce name uniqueness only on goals visible to other users
create unique index goal_name_unique_if_published on scheduler.scheduling_goal_metadata (name) where public;

comment on table scheduler.scheduling_goal_metadata is e''
  'A goal for scheduling a plan.';
comment on column scheduler.scheduling_goal_metadata.id is e''
  'The unique identifier of the goal';
comment on column scheduler.scheduling_goal_metadata.name is e''
  'A human-meaningful name.';
comment on column scheduler.scheduling_goal_metadata.description is e''
  'A detailed description suitable for long-form documentation.';
comment on column scheduler.scheduling_goal_metadata.public is e''
  'Whether this goal is visible to all users.';
comment on column scheduler.scheduling_goal_metadata.owner is e''
  'The user responsible for this goal.';
comment on column scheduler.scheduling_goal_metadata.updated_by is e''
  'The user who last modified this goal''s metadata.';
comment on column scheduler.scheduling_goal_metadata.created_at is e''
  'The time at which this goal was created.';
comment on column scheduler.scheduling_goal_metadata.updated_at is e''
  'The time at which this goal''s metadata was last modified.';

create trigger set_timestamp
before update on scheduler.scheduling_goal_metadata
for each row
execute function util_functions.set_updated_at();
