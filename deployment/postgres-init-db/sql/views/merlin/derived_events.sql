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

alter view if exists merlin.derived_events owner to aerie;
comment on view  merlin.derived_events is e''
  'A view detailing all derived events from the ';
