-- Simulation Resources
create table hasura_functions.resource_at_start_offset_return_value(
  dataset_id integer not null,
  id integer not null,
  name text not null,
  type jsonb,
  start_offset interval not null,
  dynamics jsonb,
  is_gap bool not null
);

create function hasura_functions.get_resources_at_start_offset(_dataset_id int, _start_offset interval)
returns setof hasura_functions.resource_at_start_offset_return_value
strict
stable
security invoker
language plpgsql as $$
begin
  return query
    select distinct on (p.name)
      p.dataset_id, p.id, p.name, p.type, ps.start_offset, ps.dynamics, ps.is_gap
    from profile p, profile_segment ps
	  where ps.profile_id = p.id
	    and p.dataset_id = _dataset_id
	    and ps.dataset_id = _dataset_id
	    and ps.start_offset <= _start_offset
	  order by p.name, ps.start_offset desc;
end
$$;
