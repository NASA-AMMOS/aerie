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
create table merlin.external_source (
    key text not null,
    source_type_name text not null,
    derivation_group_name text not null,
    valid_at timestamp with time zone not null,
    start_time timestamp with time zone not null,
    end_time timestamp with time zone not null,
    CHECK (end_time > start_time),
    created_at timestamp with time zone default now() not null,
    metadata jsonb,

    constraint external_source_pkey
      primary key (key, derivation_group_name),
    -- a given dg cannot have two sources with the same valid_at!
    CONSTRAINT dg_unique_valid_at UNIQUE (derivation_group_name, valid_at),
    constraint external_source_references_external_source_type_name
      foreign key (source_type_name)
      references merlin.external_source_type(name),
    constraint external_source_type_matches_derivation_group
      foreign key (derivation_group_name, source_type_name)
      references merlin.derivation_group (name, source_type_name)
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
    constraint external_event_references_source_key_derivation_group
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
    enabled boolean not null default true,
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

-- Add a trigger verifying that events fit into their sources
create or replace function merlin.check_event_times()
 	returns trigger
 	language plpgsql as
$func$
declare
	source_start timestamp with time zone;
	source_end timestamp with time zone;
	event_start timestamp with time zone;
	event_end timestamp with time zone;
begin
  	select start_time into source_start from merlin.external_source where new.source_key = external_source.key and new.derivation_group_name = external_source.derivation_group_name;
  	select end_time into source_end from merlin.external_source where new.source_key = external_source.key AND new.derivation_group_name = external_source.derivation_group_name;
    event_start := new.start_time;
	event_end := new.start_time + new.duration;
	if event_start < source_start or event_end < source_start then
		raise exception 'Event %s out of bounds of source %s', new.key, new.source_key;
	end if;
	if event_start > source_end or event_end > source_end then
		raise exception 'Event %s out of bounds of source %s', new.key, new.source_key;
	end if;
	return null;
end;
$func$;

create trigger check_event_times
after insert on merlin.external_event
	for each row execute function merlin.check_event_times();

-- Create a table to track which sources the user has and has not seen added/removed
create table ui.seen_sources
(
    username text not null,
    external_source_name text not null,
    external_source_type text not null, -- included for ease of filtering, though not part of pkey
    derivation_group text not null,

    constraint seen_sources_pkey
      primary key (username, external_source_name, derivation_group),
    constraint seen_sources_references_user
      foreign key (username)
      references permissions.users (username) match simple
);

comment on table ui.seen_sources is e''
  'A table for tracking the external sources acknowledge/unacknowledged by each user.';

-- create a function to aid the derived_events view, around diffing time ranges
create function merlin.subtract_later_ranges(curr_date tstzmultirange, later_dates tstzmultirange[])
returns tstzmultirange
language plpgsql as $$
  declare
	  ret tstzmultirange := curr_date;
	  later_date tstzmultirange;
begin
	foreach later_date in array later_dates loop
		ret := ret - later_date;
	end loop;
	return ret;
end
$$;

-- create a view that derives events from different sources in a given derivation group
create or replace view merlin.derived_events
as
select source_key,
  derivation_group_name,
  event_key,
  duration,
  event_type_name,
  start_time,
  source_range,
  valid_at
from ( select rule1_3.source_key,
        rule1_3.event_key,
        rule1_3.event_type_name,
        rule1_3.duration,
        rule1_3.derivation_group_name,
        rule1_3.start_time,
        rule1_3.source_range,
        rule1_3.valid_at,
        row_number() over (partition by rule1_3.event_key, rule1_3.derivation_group_name order by rule1_3.valid_at desc) as rn
        from ( select sub.key as source_key,
                external_event.key as event_key,
                external_event.event_type_name,
                external_event.duration,
                sub.derivation_group_name,
                external_event.start_time,
                sub.source_range,
                sub.valid_at
                from merlin.external_event
                join ( with derivation_tb_range as (
                        select external_source.key,
                                external_source.derivation_group_name,
                                tstzmultirange(tstzrange(external_source.start_time, external_source.end_time)) AS dr,
                                external_source.valid_at
                                from merlin.external_source
                                order by external_source.valid_at
                        ), ranges_with_subs as (
                        select tr1.key,
                              tr1.derivation_group_name,
                              tr1.dr as original_range,
                              coalesce(array_remove(array_agg(tr2.dr order by tr2.valid_at) FILTER (where tr1.derivation_group_name = tr2.derivation_group_name), null::tstzmultirange), '{}'::tstzmultirange[]) as subsequent_ranges,
                              tr1.valid_at
                              from derivation_tb_range tr1
                                  left join derivation_tb_range tr2 on tr1.valid_at < tr2.valid_at
                              group by tr1.key, tr1.derivation_group_name, tr1.valid_at, tr1.dr
                        )
                        select ranges_with_subs.key,
                              ranges_with_subs.derivation_group_name,
                              ranges_with_subs.original_range,
                              ranges_with_subs.subsequent_ranges,
                              merlin.subtract_later_ranges(ranges_with_subs.original_range, ranges_with_subs.subsequent_ranges) AS source_range,
                              ranges_with_subs.valid_at
                        from ranges_with_subs
                        order by ranges_with_subs.derivation_group_name desc, ranges_with_subs.valid_at) sub on sub.key = external_event.source_key and sub.derivation_group_name = external_event.derivation_group_name
                where sub.source_range @> external_event.start_time
        order by sub.derivation_group_name, external_event.start_time) rule1_3) t
where rn = 1
order by start_time;

comment on view  merlin.derived_events is e''
  'A view detailing all derived events from the ';

-- create a view that aggregates additional derivation group information
create or replace view merlin.derivation_group_comp
 as
select
	name,
	source_type_name,
	sources,
	array_agg(distinct event_type) as event_types,
	derived_total
	from (
		select
			name,
			source_type_name,
			array_agg(distinct concat(sources.source_key, ', ', sources.derivation_group_name, ', ', sources.contained_events)) as sources,
			UNNEST(event_types) as event_type,
			count(distinct c.event_key) as derived_total
			from merlin.derivation_group
			join (
				select
					source_key,
					derivation_group_name,
					count(key) as contained_events
				from merlin.external_event
				group by source_key, derivation_group_name
				order by source_key
			) sources on sources.derivation_group_name = derivation_group.name
			join (
				select
					source_key,
					derivation_group_name,
					array_agg(distinct event_type_name) as event_types
				from merlin.external_event
				group by source_key, derivation_group_name
				order by source_key
			) types on types.derivation_group_name = derivation_group.name
			left join (
				select
					derived_events.event_key,
					derived_events.derivation_group_name
			  	from merlin.derived_events
			) c on c.derivation_group_name = derivation_group.name
			group by name, source_type_name, event_types
	)
	group by name, source_type_name, sources, derived_total;

comment on view  merlin.derivation_group_comp is e''
  'A view detailing all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. As such, a new view was created to lighten the load.';

call migrations.mark_migration_applied('9');
