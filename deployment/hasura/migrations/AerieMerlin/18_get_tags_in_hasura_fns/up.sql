-- UNLOCK
alter table hasura_functions.begin_merge_return_value
  drop column non_conflicting_activities,
  drop column conflicting_activities;

-- UPDATE TABLES
alter table hasura_functions.get_non_conflicting_activities_return_value
  add column source_tags jsonb,
  add column target_tags jsonb;

alter table hasura_functions.get_conflicting_activities_return_value
  add column source_tags jsonb,
  add column target_tags jsonb,
  add column merge_base_tags jsonb;

-- UPDATE FUNCTIONS
create or replace function hasura_functions.get_non_conflicting_activities(merge_request_id integer)
  returns setof hasura_functions.get_non_conflicting_activities_return_value
  strict
  language plpgsql stable as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
begin
  select snapshot_id_supplying_changes, plan_id_receiving_changes
  from merge_request
  where merge_request.id = $1
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes;

  return query
    with plan_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, adt.directive_id
      from metadata.tags tags, metadata.activity_directive_tags adt
      where tags.id = adt.tag_id
        and adt.plan_id = _plan_id_receiving_changes
      group by adt.directive_id
    ),
    snapshot_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, sat.directive_id
      from metadata.tags tags, metadata.snapshot_activity_tags sat
      where tags.id = sat.tag_id
        and sat.snapshot_id = _snapshot_id_supplying_changes
      group by sat.directive_id
    )
    select
      activity_id,
      change_type,
      snap_act,
      act,
      coalesce(st.tags, '[]'),
      coalesce(pt.tags, '[]')
    from
      (select msa.activity_id, msa.change_type
       from merge_staging_area msa
       where msa.merge_request_id = $1) c
        left join plan_snapshot_activities snap_act
               on _snapshot_id_supplying_changes = snap_act.snapshot_id
              and c.activity_id = snap_act.id
        left join activity_directive act
               on _plan_id_receiving_changes = act.plan_id
              and c.activity_id = act.id
        left join plan_tags pt
               on c.activity_id = pt.directive_id
        left join snapshot_tags st
               on c.activity_id = st.directive_id;
end
$$;

create or replace function hasura_functions.get_conflicting_activities(merge_request_id integer)
  returns setof hasura_functions.get_conflicting_activities_return_value
  strict
  language plpgsql stable as $$
declare
  _snapshot_id_supplying_changes integer;
  _plan_id_receiving_changes integer;
  _merge_base_snapshot_id integer;
begin
  select snapshot_id_supplying_changes, plan_id_receiving_changes, merge_base_snapshot_id
  from merge_request
  where merge_request.id = $1
  into _snapshot_id_supplying_changes, _plan_id_receiving_changes, _merge_base_snapshot_id;

  return query
    with plan_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, adt.directive_id
      from metadata.tags tags, metadata.activity_directive_tags adt
      where tags.id = adt.tag_id
        and _plan_id_receiving_changes = adt.plan_id
      group by adt.directive_id
    ), snapshot_tags as (
      select jsonb_agg(json_build_object(
        'id', id,
        'name', name,
        'color', color,
        'owner', owner,
        'created_at', created_at
        )) as tags, sdt.directive_id, sdt.snapshot_id
      from metadata.tags tags, metadata.snapshot_activity_tags sdt
      where tags.id = sdt.tag_id
        and (sdt.snapshot_id = _snapshot_id_supplying_changes
         or sdt.snapshot_id = _merge_base_snapshot_id)
      group by sdt.directive_id, sdt.snapshot_id
    )
    select
      activity_id,
      change_type_supplying,
      change_type_receiving,
      case
        when c.resolution = 'supplying' then 'source'::resolution_type
        when c.resolution = 'receiving' then 'target'::resolution_type
        when c.resolution = 'none' then 'none'::resolution_type
      end,
      snap_act,
      act,
      merge_base_act,
      coalesce(st.tags, '[]'),
      coalesce(pt.tags, '[]'),
      coalesce(mbt.tags, '[]')
    from
      (select * from conflicting_activities c where c.merge_request_id = $1) c
        left join plan_snapshot_activities merge_base_act
                  on c.activity_id = merge_base_act.id and _merge_base_snapshot_id = merge_base_act.snapshot_id
        left join plan_snapshot_activities snap_act
                  on c.activity_id = snap_act.id and _snapshot_id_supplying_changes = snap_act.snapshot_id
        left join activity_directive act
                  on _plan_id_receiving_changes = act.plan_id and c.activity_id = act.id
        left join plan_tags pt
                  on c.activity_id = pt.directive_id
        left join snapshot_tags st
                  on c.activity_id = st.directive_id and _snapshot_id_supplying_changes = st.snapshot_id
        left join snapshot_tags mbt
                  on c.activity_id = st.directive_id and _merge_base_snapshot_id = st.snapshot_id;
end;
$$;

-- LOCK
alter table hasura_functions.begin_merge_return_value
  add column non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[],
  add column conflicting_activities hasura_functions.get_conflicting_activities_return_value[];

-- ADD TRIGGERS
create function adt_check_locked_new()
  returns trigger
  security definer
  language plpgsql as $$
  begin
    call plan_locked_exception(new.plan_id);
    return new;
  end $$;
create function adt_check_locked_old()
  returns trigger
  security definer
  language plpgsql as $$
  begin
    call plan_locked_exception(old.plan_id);
    return old;
  end $$;

create trigger adt_check_plan_locked_insert_update
  before insert or update on metadata.activity_directive_tags
  for each row
  execute procedure adt_check_locked_new();
create trigger adt_check_plan_locked_update_delete
  before update or delete on metadata.activity_directive_tags
  for each row
  execute procedure adt_check_locked_old();

create function snapshot_tags_in_review_delete()
  returns trigger
  security definer
language plpgsql as $$
  begin
    if exists(select status from merge_request mr
      where
        (mr.snapshot_id_supplying_changes = old.snapshot_id
        or mr.merge_base_snapshot_id = old.snapshot_id)
      and mr.status = 'in-progress') then
      raise exception 'Cannot delete. Snapshot is in use in an active merge review.';
    end if;
    return old;
  end
$$;

create trigger snapshot_tags_in_review_delete_trigger
  before delete on metadata.snapshot_activity_tags
  for each row
  execute function snapshot_tags_in_review_delete();

call migrations.mark_migration_applied('18');
