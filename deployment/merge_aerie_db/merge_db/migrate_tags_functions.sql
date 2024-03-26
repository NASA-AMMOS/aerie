create or replace function tags.tag_ids_activity_snapshot(_directive_id integer, _snapshot_id integer)
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

create or replace function tags.tag_ids_activity_directive(_directive_id integer, _plan_id integer)
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

create or replace function tags.get_tags(_activity_id int, _plan_id int)
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

create or replace function tags.adt_check_locked_new()
  returns trigger
  security definer
  language plpgsql as $$
begin
  call merlin.plan_locked_exception(new.plan_id);
  return new;
end $$;

create or replace function tags.adt_check_locked_old()
  returns trigger
  security definer
  language plpgsql as $$
begin
  call merlin.plan_locked_exception(old.plan_id);
  return old;
end $$;

create or replace function tags.snapshot_tags_in_review_delete()
  returns trigger
  security definer
language plpgsql as $$
begin
  if exists(select status from merlin.merge_request mr
            where
              (mr.snapshot_id_supplying_changes = old.snapshot_id
                 or mr.merge_base_snapshot_id = old.snapshot_id)
              and mr.status = 'in-progress') then
    raise exception 'Cannot delete. Snapshot is in use in an active merge review.';
  end if;
  return old;
end
$$;
