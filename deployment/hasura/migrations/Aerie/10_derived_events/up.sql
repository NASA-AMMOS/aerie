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

alter function merlin.subtract_later_ranges(tstzmultirange, tstzmultirange[])
    owner to aerie;
grant execute on function merlin.subtract_later_ranges(tstzmultirange, tstzmultirange[]) to aerie;

--create or replace view merlin.derived_events
--  as
--  select
--      source_id,
--      event_id,
--      event_key,
--      event_type_name,
--      derivation_group_name,
--      start_time,
--      source_range,
--      valid_at
--     from ( select rule1_3.source_id,
--              rule1_3.event_id,
--              rule1_3.event_key,
--              rule1_3.event_type_name,
--              rule1_3.derivation_group_name,
--              rule1_3.start_time,
--              rule1_3.source_range,
--              rule1_3.valid_at,
--              row_number() over (partition by rule1_3.event_key, rule1_3.derivation_group_name order by rule1_3.valid_at desc) as rn
--              from ( select sub.id as source_id,
--                      external_event.id as event_id,
--                      external_event.key as event_key,
--                      external_event.event_type_name,
--                      sub.derivation_group_name,
--                      external_event.start_time,
--                      sub.source_range,
--                      sub.valid_at
--                      from merlin.external_event
--                        join (
--  	 					with derivation_tb_range as (
--  							select id, key, derivation_group_name, tstzmultirange(tstzrange(start_time, end_time)) as dr, valid_at
--  								from merlin.external_source
--  								order by valid_at asc
--  						),
--  						ranges_with_subs as (
--  							select tr1.id, tr1.derivation_group_name, tr1.key, tr1.dr as original_range, COALESCE(array_remove(array_agg(tr2.dr order by tr2.valid_at) filter (where tr1.derivation_group_name = tr2.derivation_group_name), null), '{}') as subsequent_ranges, tr1.valid_at
--  								from derivation_tb_range tr1
--  								left join derivation_tb_range tr2 on tr1.valid_at < tr2.valid_at
--  								group by tr1.id, tr1.derivation_group_name, tr1.key, tr1.valid_at, tr1.dr
--  						)
--  						select id, derivation_group_name, key, original_range, subsequent_ranges, merlin.subtract_later_ranges(original_range, subsequent_ranges) as source_range, valid_at
--  							from ranges_with_subs
--  							order by derivation_group_name desc, valid_at asc
--  	 				) sub on sub.id = external_event.source_id
--                    where sub.source_range @> external_event.start_time
--                    order by sub.derivation_group_name, external_event.start_time) rule1_3) t
--    where rn = 1
--    order by start_time;
--
--alter view if exists merlin.derived_events owner to aerie;
--comment on view  merlin.derived_events is e''
--  'A view detailing all derived events from the ';
--
--create or replace view merlin.derivation_group_comp
-- as
--select derivation_group.name,
--   derivation_group.source_type_name,
--   array_agg(distinct concat(sources.source_id, ', ', sources.key, ', ', sources.contained_events)) as sources,
--   array_agg(distinct sources.type) as event_types,
--   count(distinct c.event_key) as derived_total
--  from merlin.derivation_group
--    left join ( select external_source.id as source_id,
--           external_source.key,
--           count(a.event_key) as contained_events,
--		   external_event_type.name as type,
--           external_source.derivation_group_name,
--           external_source.valid_at
--          from merlin.external_source
--            join ( select external_event.source_id,
--					external_event.event_type_name,
--                   external_event.key as event_key
--                  from merlin.external_event) a on a.source_id = external_source.id
--			join merlin.external_event_type on external_event_type.name = a.event_type_name
--         group by external_source.id, external_source.key, external_source.derivation_group_name, external_source.valid_at, type) sources on sources.derivation_group_name = derivation_group.name
--    left join ( select derived_events.event_key,
--           derived_events.derivation_group_name
--          from merlin.derived_events) c on c.derivation_group_name = derivation_group.name
-- group by derivation_group.name, derivation_group.source_type_name;
--
--alter view if exists merlin.derivation_group_comp owner to aerie;
--comment on view  merlin.derivation_group_comp is e''
--  'A view detailing all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. As such, a new view was created to lighten the load.';

call migrations.mark_migration_applied('10');
