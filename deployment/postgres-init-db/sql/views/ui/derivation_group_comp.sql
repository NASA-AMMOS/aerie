-- create a view that aggregates additional derivation group information
create view merlin.derivation_group_comp
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

comment on view  merlin.derivation_group_comp is e''
  'A view detailing all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. as such, a new view was created to lighten the load.';
