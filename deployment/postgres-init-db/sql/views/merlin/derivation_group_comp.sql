create or replace view merlin.derivation_group_comp
 as
select derivation_group.name,
   derivation_group.source_type_name,
   array_agg(distinct concat(sources.source_id, ', ', sources.key, ', ', sources.contained_events)) as sources,
   array_agg(distinct sources.type) as event_types,
   count(distinct c.event_key) as derived_total
  from merlin.derivation_group
    left join ( select external_source.id as source_id,
           external_source.key,
           count(a.event_key) as contained_events,
		   external_event_type.name as type,
           external_source.derivation_group_name,
           external_source.valid_at
          from merlin.external_source
            join ( select external_event.source_id,
					external_event.event_type_name,
                   external_event.key as event_key
                  from merlin.external_event) a on a.source_id = external_source.id
			join merlin.external_event_type on external_event_type.name = a.event_type_name
         group by external_source.id, external_source.key, external_source.derivation_group_name, external_source.valid_at, type) sources on sources.derivation_group_name = derivation_group.name
    left join ( select derived_events.event_key,
           derived_events.derivation_group_name
          from merlin.derived_events) c on c.derivation_group_name = derivation_group.name
 group by derivation_group.name, derivation_group.source_type_name;

alter view if exists merlin.derivation_group_comp owner to aerie;
comment on view  merlin.derivation_group_comp is e''
  'A view detailing all relevant information for derivation groups. This was created as we wanted all of this information, but had many heavyweight subscriptions and queries to get this desired result. As such, a new view was created to lighten the load.';
