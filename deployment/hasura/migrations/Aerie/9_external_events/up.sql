-- Create table for external source types
create table merlin.external_source_type (
    name text not null,

    constraint external_source_type_pkey
      primary key (name)
);

comment on table merlin.external_source_type is e''
  'A table for externally imported event source types.';

-- Create table for external event types
create table merlin.external_event_type (
    name text not null,

    constraint external_event_type_pkey
      primary key (name)
);

comment on table merlin.external_event_type is e''
  'A table for externally imported event types.';

-- Create a table to represent derivation groups for external sources
create table merlin.derivation_group (
    name text not null unique,
    source_type_name text not null,

    constraint derivation_group_pkey
      primary key (name, source_type_name),
    constraint derivation_group_references_external_source_type
      foreign key (source_type_name)
      references merlin.external_source_type(name)
);
comment on table merlin.derivation_group is e''
  'A table to represent the names of groups of sources to run derivation operations over.';

-- Create a table to represent external event sources.
create table merlin.external_event (
    key text not null,
    event_type_name text not null,
	  source_key text not null,
	  source_type_name text not null,
    derivation_group_name text not null,
    start_time timestamp with time zone not null,
    duration interval not null,
    properties jsonb,

    constraint external_event_pkey
      primary key (key, source_type_name, source_key, derivation_group_name, event_type_name),
    constraint external_event_references_source_id
      foreign key (source_key, source_type_name, derivation_group_name)
      references merlin.external_source (key, source_type_name, derivation_group_name),
    constraint external_event_references_event_type_name
      foreign key (event_type_name)
      references merlin.external_event_type(name)
);

comment on table merlin.external_source is e''
  'A table for externally imported event sources.';

-- Create table for external events
create table merlin.external_event (
    key text not null,
    event_type_name text not null,
	source_key text not null,
    derivation_group_name text not null,
    start_time timestamp with time zone not null,
    duration interval not null,
    properties jsonb,

    constraint external_event_pkey
      primary key (key, source_key, derivation_group_name, event_type_name),
    constraint external_event_references_source_id
      foreign key (source_key, derivation_group_name)
      references merlin.external_source (key, derivation_group_name),
    constraint external_event_references_event_type_name
      foreign key (event_type_name)
      references merlin.external_event_type(name)
);

comment on table merlin.external_event is e''
  'A table for externally imported events.';

-- Create table for plan/external event links
create table merlin.plan_derivation_group (
    plan_id integer not null,
    derivation_group_name text not null,
    created_at timestamp with time zone default now() not null,

    constraint plan_derivation_group_pkey
      primary key (plan_id, derivation_group_name),
    constraint plan_derivation_group_references_plan_id
      foreign key (plan_id)
      references merlin.plan(id),
    constraint plan_derivation_group_references_derivation_group_name
      foreign key (derivation_group_name)
      references merlin.derivation_group(name)
);

comment on table merlin.plan_derivation_group is e''
  'A table for linking externally imported event sources & plans.';

-- Create a table to track which sources the user has and has not seen added/removed
create table ui.seen_sources
(
    username text not null,
    external_source_name text not null,
    external_source_type text not null,
    derivation_group text not null,

    constraint seen_sources_pkey
      primary key (username, external_source_name, derivation_group), -- is this a good pkey?
    constraint seen_sources_references_user
      foreign key (username)
      references permissions.users (username) match simple
);

comment on table ui.seen_sources is e''
  'A table for tracking the external sources acknowledge/unacknowledged by each user.';


call migrations.mark_migration_applied('9');
