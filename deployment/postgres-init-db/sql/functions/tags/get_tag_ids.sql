create function tags.tag_ids_activity_snapshot(_directive_id integer, _snapshot_id integer)
  returns int[]
  language plpgsql as $$
  declare
    tags int[];
begin
  select array_agg(tag_id)
  from tags.snapshot_activity_tags sat
  where sat.snapshot_id = _snapshot_id
    and sat.directive_id = _directive_id
  into tags;
  return tags;
end
$$;

create function tags.tag_ids_activity_directive(_directive_id integer, _plan_id integer)
  returns int[]
  language plpgsql as $$
  declare
    tags int[];
begin
  select array_agg(tag_id)
  from tags.activity_directive_tags adt
  where adt.plan_id = _plan_id
    and adt.directive_id = _directive_id
  into tags;
  return tags;
end
$$;
