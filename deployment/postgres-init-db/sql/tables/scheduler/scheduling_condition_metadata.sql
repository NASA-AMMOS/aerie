create table scheduler.scheduling_condition_metadata (
  id integer generated always as identity,

  name text not null,
  description text not null default '',
  public boolean not null default false,

  owner text,
  updated_by text,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint scheduling_condition_metadata_pkey
    primary key (id)
);

-- A partial index is used to enforce name uniqueness only on conditions visible to other users
create unique index condition_name_unique_if_published on scheduler.scheduling_condition_metadata (name) where public;

comment on table scheduler.scheduling_condition_metadata is e''
  'A condition restricting scheduling of a plan.';
comment on column scheduler.scheduling_condition_metadata.id is e''
  'The unique identifier for this scheduling condition.';
comment on column scheduler.scheduling_condition_metadata.name is e''
  'A short human readable name for this condition';
comment on column scheduler.scheduling_condition_metadata.description is e''
  'A longer text description of this scheduling condition.';
comment on column scheduler.scheduling_condition_metadata.public is e''
  'Whether this goal is visible to all users.';
comment on column scheduler.scheduling_condition_metadata.owner is e''
  'The user responsible for this condition.';
comment on column scheduler.scheduling_condition_metadata.updated_by is e''
  'The user who last modified this condition''s metadata.';
comment on column scheduler.scheduling_condition_metadata.created_at is e''
  'The time at which this condition was created.';
comment on column scheduler.scheduling_condition_metadata.updated_at is e''
  'The time at which this condition''s metadata was last modified.';

create trigger set_timestamp
before update on scheduler.scheduling_condition_metadata
for each row
execute function util_functions.set_updated_at();
