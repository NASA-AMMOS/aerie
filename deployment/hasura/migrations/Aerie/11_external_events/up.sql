create table merlin.external_source_type (
    name text not null,

    constraint external_source_type_pkey
      primary key (name)
);

comment on table merlin.external_source_type is e''
  'Externally imported event source types.';

comment on column merlin.external_source_type.name is e''
  'The identifier for this external_source_type, as well as its name.';

create table merlin.external_event_type (
    name text not null,

    constraint external_event_type_pkey
      primary key (name)
);

comment on table merlin.external_event_type is e''
  'Externally imported event types.';

comment on column merlin.external_event_type.name is e''
  'The identifier for this external_event_type, as well as its name.';

create table merlin.derivation_group (
    name text not null unique,
    source_type_name text not null,

    constraint derivation_group_pkey
      primary key (name),
    constraint derivation_group_references_external_source_type
      foreign key (source_type_name)
      references merlin.external_source_type(name)
);

comment on table merlin.derivation_group is e''
  'A representation of the names of groups of sources to run derivation operations over.';

comment on column merlin.derivation_group.name is e''
  'The name and primary key of the derivation group.';
comment on column merlin.derivation_group.source_type_name is e''
  'The name of the external_source_type of sources in this derivation group.';

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
    -- TODO: going forward, we might want to consider making an exception to the above if sources have no overlap. That
    --        being said, this may be overkill or an unnecessary complication to the general rule.
    constraint external_source_references_external_source_type_name
      foreign key (source_type_name)
      references merlin.external_source_type(name),
    constraint external_source_type_matches_derivation_group
      foreign key (derivation_group_name)
      references merlin.derivation_group (name)
);

comment on table merlin.external_source is e''
  'Externally imported event sources.';

comment on column merlin.external_source.key is e''
  'The key, or name, of the external_source.\n'
  'Part of the primary key, along with the derivation_group_name';
comment on column merlin.external_source.source_type_name is e''
  'The type of this external_source.';
comment on column merlin.external_source.derivation_group_name is e''
  'The name of the derivation_group that this external_source is included in.';
comment on column merlin.external_source.valid_at is e''
  'The time (in _planner_ time, NOT plan time) at which a source becomes valid.\n'
  'This time helps determine when a source''s events are valid for the span of time it covers.';
comment on column merlin.external_source.start_time is e''
  'The start time (in _plan_ time, NOT planner time), of the range that this source describes.';
comment on column merlin.external_source.end_time is e''
  'The end time (in _plan_ time, NOT planner time), of the range that this source describes.';
comment on column merlin.external_source.created_at is e''
  'The time (in _planner_ time, NOT plan time) that this particular source was created.\n'
  'This column is used primarily for documentation purposes, and has no associated functionality.';
comment on column merlin.external_source.metadata is e''
  'Any metadata or additional data associated with this version that a data originator may have wanted included.\n'
  'Like the ''created_at'' column, this column is used primarily for documentation purposes, and has no associated functionality.';

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
  'Externally imported events.';

comment on column merlin.external_event.key is e''
  'The key, or name, of the external_event.\n'
  'Part of the primary key, along with the source_key, derivation_group_name, and event_type_name.';
comment on column merlin.external_event.event_type_name is e''
  'The type of this external_event.';
comment on column merlin.external_event.source_key is e''
  'The key of the external_source that this external_event is included in.\n'
  'Used as a foreign key along with the derivation_group_name to directly identify said source.\n'
  'Part of the primary key along with the key, derivation_group_name, and event_type_name.';
comment on column merlin.external_event.derivation_group_name is e''
  'The derivation_group that the external_source bearing this external_event is a part of.';
comment on column merlin.external_event.start_time is e''
  'The start time (in _plan_ time, NOT planner time), of the range that this source describes.';
comment on column merlin.external_event.duration is e''
  'The span of time of this external event.';
comment on column merlin.external_event.properties is e''
  'Any properties or additional data associated with this version that a data originator may have wanted included.\n'
  'This column is used primarily for documentation purposes, and has no associated functionality.';

create table merlin.plan_derivation_group (
    plan_id integer not null,
    derivation_group_name text not null,

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
  'Links externally imported event sources & plans.';

comment on column merlin.plan_derivation_group.plan_id is e''
  'The plan with which the derivation group is associated.';
comment on column merlin.plan_derivation_group.derivation_group_name is e''
  'The derivation group being associated with the plan.';

-- if an external source is linked to a plan it cannot be deleted
create function merlin.check_if_associated()
  returns trigger
  language plpgsql as $$
begin
  if exists(select * from merlin.plan_derivation_group pdg where pdg.derivation_group_name = old.derivation_group_name) then
    raise foreign_key_violation
    using message='External source ' || old.key || ' is part of a derivation group that is associated to a plan.';
  end if;
  return null;
end;
$$;

create trigger check_if_associated
before delete on merlin.external_source
  for each row execute function merlin.check_if_associated();

create function merlin.check_external_event_boundaries()
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
		raise exception 'Event % out of bounds of source %', new.key, new.source_key;
	end if;
	if event_start > source_end or event_end > source_end then
		raise exception 'Event % out of bounds of source %', new.key, new.source_key;
	end if;
	return null;
end;
$func$;

comment on function merlin.check_external_event_boundaries() is e''
  'Checks that an external_event added to the database has a start time and duration that fall in bounds of the associated external_source.';

create trigger check_external_event_boundaries
after insert on merlin.external_event
	for each row execute function merlin.check_external_event_boundaries();

comment on trigger check_external_event_boundaries on merlin.external_event is e''
  'Fires any time a new external event is added that checks that the span of the event fits in its referenced source.';

create table ui.seen_sources
(
    plan_id integer not null,
    derivation_group_name text not null,
    last_acknowledged_at timestamp with time zone default now() not null,

    constraint seen_sources_pkey
      primary key (plan_id, derivation_group_name),
    constraint seen_sources_references_plan_derivation_group
      foreign key (plan_id, derivation_group_name)
      references merlin.plan_derivation_group (plan_id, derivation_group_name)
      on delete cascade
);

comment on table ui.seen_sources is e''
  'Tracks whether a plan (specifically any of its contributors/owners) has acknowledged that a source is now associated with a plan by virtue of being a member of an associated derivation group.\n'
  'Membership indicates that the new source has been acknowledged and is now understood to be a member.\n'
  'A source in external_source that is part of a derivation group associated with this plan but not in this table is unacknowledged.\n'
  'Acknowledgements are performed in the UI, and upon doing so new entries are appended to this table.';

comment on column ui.seen_sources.plan_id is e''
  'The plan that any new source is now associated with by virtue of being a member of the named derivation group.';
comment on column ui.seen_sources.derivation_group_name is e''
  'The derivation group of the plan is associated with.';
comment on column ui.seen_sources.last_acknowledged_at is e''
  'The time at which changes to the derivation group were last acknowledged.';

-- add a trigger that adds to seen sources whenever an association is made
create function ui.add_seen_source_on_assoc()
  returns trigger
  language plpgsql as $$
begin
  insert into ui.seen_sources values (new.plan_id, new.derivation_group_name);
  return new;
end;
$$;

create trigger add_seen_source_on_assoc
after insert on merlin.plan_derivation_group
  for each row execute function ui.add_seen_source_on_assoc();

create function merlin.subtract_later_ranges(curr_date tstzmultirange, later_dates tstzmultirange[])
returns tstzmultirange
immutable
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

comment on function merlin.subtract_later_ranges(curr_date tstzmultirange, later_dates tstzmultirange[]) is e''
  'Used by the derived_events view that produces from the singular interval of time that a source covers a set of disjoint intervals.\n'
  'The disjointedness arises from where future sources'' spans are subtracted from this one.\n'
  'For example, if a source is valid at t=0, and covers span s=1 to s=5, and there is a source valid at t=1 with a span s=2 to s=3\n'
  'and another valid at t=2 with a span 3 to 4, then this source should have those spans subtracted and should only be valid over [1,2] and [4,5].';

create view merlin.derived_events
as
select
  -- from the events adhering to rules 1-3, filter by overlapping names such that only the most recent and valid event is included (row_number = 1; fitting rule 4)
  source_key,
  derivation_group_name,
  event_key,
  duration,
  event_type_name,
  start_time,
  properties,
  source_range,
  valid_at
from ( -- select all relevant properties of those shortlisted in the from clause (rule1_3), and create an ordering based on overlapping names and valid_at (row_number) to adhere to rule 4
        select rule1_3.source_key,
        rule1_3.event_key,
        rule1_3.event_type_name,
        rule1_3.duration,
        rule1_3.derivation_group_name,
        rule1_3.properties,
        rule1_3.start_time,
        rule1_3.source_range,
        rule1_3.valid_at,
        row_number() over (partition by rule1_3.event_key, rule1_3.derivation_group_name order by rule1_3.valid_at desc) as rn
        from (
                -- select the events from the sources and include them as they fit into the ranges determined by sub
                select sub.key as source_key,
                external_event.key as event_key,
                external_event.event_type_name,
                external_event.duration,
                sub.derivation_group_name,
                external_event.start_time,
                external_event.properties,
                sub.source_range,
                sub.valid_at
                from merlin.external_event
                join ( with derivation_tb_range as (
                        -- this inner selection (derivation_tb_range) orders sources by their valid time and extracts the multirange that they are stated to be valid over
                        select external_source.key,
                                external_source.derivation_group_name,
                                tstzmultirange(tstzrange(external_source.start_time, external_source.end_time)) AS dr,
                                external_source.valid_at
                                from merlin.external_source
                                order by external_source.valid_at
                        ), ranges_with_subs as (
                        -- this inner selection (ranges_with_subs) takes each of the sources above and compiles a list of all the sources that follow it and their multiranges that they are stated to be valid over
                        select tr1.key,
                              tr1.derivation_group_name,
                              tr1.dr as original_range,
                              coalesce(array_remove(array_agg(tr2.dr order by tr2.valid_at) FILTER (where tr1.derivation_group_name = tr2.derivation_group_name), null::tstzmultirange), '{}'::tstzmultirange[]) as subsequent_ranges,
                              tr1.valid_at
                              from derivation_tb_range tr1
                                  left join derivation_tb_range tr2 on tr1.valid_at < tr2.valid_at
                              group by tr1.key, tr1.derivation_group_name, tr1.valid_at, tr1.dr
                        )
                        -- this final selection (sub) utilizes the first, as well as merlin.subtract_later_ranges, to produce a sparse multirange that a given source is valid over. See merlin.subtract_later_ranges for further details on subtracted ranges.
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
  'Details all derived events from all derivation groups.';

create view ui.derivation_group_comp
  as
select
  name,
  source_type_name,
  array_agg(distinct concat(sources.source_key, ', ', sources.derivation_group_name, ', ', sources.contained_events)) as sources,
  array_remove(array_agg(distinct event_type_name), null) as event_types,
	count(distinct counted.event_key) as derived_total
from (
	select derivation_group.name,
		     derivation_group.source_type_name,
		     types.event_type_name
  from merlin.derivation_group
	left outer join ( select external_event.source_key,
	                         external_event.derivation_group_name,
	                         external_event.event_type_name
			              from merlin.external_event
			              order by external_event.source_key) types on types.derivation_group_name = derivation_group.name
	group by derivation_group.name, derivation_group.source_type_name, types.event_type_name
) with_event_types
full outer join ( select external_event.source_key,
                         external_event.derivation_group_name,
                         count(external_event.key) as contained_events
                  from merlin.external_event
                  group by external_event.source_key, external_event.derivation_group_name
                  order by external_event.source_key) sources on sources.derivation_group_name = with_event_types.name
full outer join ( select derived_events.event_key,
				                 derived_events.derivation_group_name
			            from merlin.derived_events) counted on counted.derivation_group_name = with_event_types.name
group by with_event_types.name, with_event_types.source_type_name;

comment on view ui.derivation_group_comp is e''
  'Details all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. as such, a new view was created to lighten the load.';

call migrations.mark_migration_applied('11');
