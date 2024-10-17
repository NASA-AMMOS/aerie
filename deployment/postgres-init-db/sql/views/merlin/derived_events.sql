-- Rule 1. An External Event superseded by nothing will be present in the final, derived result.
-- Rule 2. An External Event partially superseded by a later External Source, but whose start time occurs before the start of said External Source(s), will be present in the final, derived result.
-- Rule 3. An External Event whose start is superseded by another External Source, even if its end occurs after the end of said External Source, will be replaced by the contents of that External Source (whether they are blank spaces, or other events).
-- Rule 4. An External Event who shares an ID with an External Event in a later External Source will always be replaced.

create materialized view merlin.derived_events
as
select
  -- from the events adhering to rules 1-3, filter by overlapping names such that only the most recent and valid event is included (row_number = 1; fitting rule 4)
  event_key,
  source_key,
  derivation_group_name,
  event_type_name,
  duration,
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

-- create a unique index, which allows concurrent refreshes
create unique index on merlin.derived_events (
  event_key,
  source_key,
  derivation_group_name,
  event_type_name
);

-- refresh the materialized view after insertion/update/deletion to external_source and external_event and derivation_group
create function merlin.refresh_derived_events_on_trigger()
  returns trigger
  language plpgsql as $$
begin
  refresh materialized view concurrently merlin.derived_events;
  return new;
end;
$$;

-- events are the most basic source of information, so we have to trigger on their insertion
create trigger refresh_derived_events_on_external_event
after insert or update or delete on merlin.external_event
  for each statement execute function merlin.refresh_derived_events_on_trigger();

-- also trigger on external sources, especially in the case of an empty source, which could still overlap and erase some
--    events in time (see "rule3_empty" test in ExternalEventTests.java).
create trigger refresh_derived_events_on_external_source
after insert or update or delete on merlin.external_source
  for each statement execute function merlin.refresh_derived_events_on_trigger();

create trigger refresh_derived_events_on_derivation_group
after insert or update or delete on merlin.derivation_group
  for each statement execute function merlin.refresh_derived_events_on_trigger();

comment on materialized view merlin.derived_events is e''
  'Details all derived events from all derivation groups.';
