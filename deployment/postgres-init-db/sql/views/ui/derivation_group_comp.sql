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

alter view if exists merlin.derivation_group_comp owner to aerie;
comment on view  merlin.derivation_group_comp is e''
  'A view detailing all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. As such, a new view was created to lighten the load.';
