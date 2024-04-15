create function tags.get_tags(_activity_id int, _plan_id int)
  returns jsonb
  security invoker
  language plpgsql as $$
  declare
    tags jsonb;
begin
    select  jsonb_agg(json_build_object(
      'id', id,
      'name', name,
      'color', color,
      'owner', owner,
      'created_at', created_at
      ))
    from tags.tags tags, tags.activity_directive_tags adt
    where tags.id = adt.tag_id
      and (adt.directive_id, adt.plan_id) = (_activity_id, _plan_id)
    into tags;
    return tags;
end
$$;
