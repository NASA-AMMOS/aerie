--------------- FUNCTIONS ---------------
-- BEGIN MERGE
drop procedure begin_merge(_merge_request_id integer, _reviewer integer);
create procedure begin_merge(_merge_request_id integer, review_username text)
  language plpgsql as $$
  declare
    validate_id integer;
    validate_status merge_request_status;
    validate_non_no_op_status activity_change_type;
    snapshot_id_supplying integer;
    plan_id_receiving integer;
    merge_base_id integer;
begin
  -- validate id and status
  select id, status
    from merge_request
    where _merge_request_id = id
    into validate_id, validate_status;

  if validate_id is null then
    raise exception 'Request ID % is not present in merge_request table.', _merge_request_id;
  end if;

  if validate_status != 'pending' then
    raise exception 'Cannot begin request. Merge request % is not in pending state.', _merge_request_id;
  end if;

  -- select from merge-request the snapshot_sc (s_sc) and plan_rc (p_rc) ids
  select plan_id_receiving_changes, snapshot_id_supplying_changes
    from merge_request
    where id = _merge_request_id
    into plan_id_receiving, snapshot_id_supplying;

  -- ensure the plan receiving changes isn't locked
  if (select is_locked from plan where plan.id=plan_id_receiving) then
    raise exception 'Cannot begin merge request. Plan to receive changes is locked.';
  end if;

  -- lock plan_rc
  update plan
    set is_locked = true
    where plan.id = plan_id_receiving;

  -- get merge base (mb)
  select get_merge_base(plan_id_receiving, snapshot_id_supplying)
    into merge_base_id;

  -- update the status to "in progress"
  update merge_request
    set status = 'in-progress',
    merge_base_snapshot_id = merge_base_id,
    reviewer_username = review_username
    where id = _merge_request_id;


  -- perform diff between mb and s_sc (s_diff)
    -- delete is B minus A on key
    -- add is A minus B on key
    -- A intersect B is no op
    -- A minus B on everything except everything currently in the table is modify
  create temp table supplying_diff(
    activity_id integer,
    change_type activity_change_type not null
  );

  insert into supplying_diff (activity_id, change_type)
  select activity_id, 'delete'
  from(
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = merge_base_id
    except
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = snapshot_id_supplying) a;

  insert into supplying_diff (activity_id, change_type)
  select activity_id, 'add'
  from(
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = snapshot_id_supplying
    except
    select id as activity_id
    from plan_snapshot_activities
      where snapshot_id = merge_base_id) a;

  insert into supplying_diff (activity_id, change_type)
    select activity_id, 'none'
      from(
        select psa.id as activity_id, name, metadata.tag_ids_activity_snapshot(psa.id, merge_base_id),
               source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
               last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities psa
        where psa.snapshot_id = merge_base_id
    intersect
      select id as activity_id, name, metadata.tag_ids_activity_snapshot(psa.id, snapshot_id_supplying),
             source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
             last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities psa
        where psa.snapshot_id = snapshot_id_supplying) a;

  insert into supplying_diff (activity_id, change_type)
    select activity_id, 'modify'
    from(
      select id as activity_id from plan_snapshot_activities
        where snapshot_id = merge_base_id or snapshot_id = snapshot_id_supplying
      except
      select activity_id from supplying_diff) a;

  -- perform diff between mb and p_rc (r_diff)
  create temp table receiving_diff(
     activity_id integer,
     change_type activity_change_type not null
  );

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'delete'
  from(
        select id as activity_id
        from plan_snapshot_activities
        where snapshot_id = merge_base_id
        except
        select id as activity_id
        from activity_directive
        where plan_id = plan_id_receiving) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'add'
  from(
        select id as activity_id
        from activity_directive
        where plan_id = plan_id_receiving
        except
        select id as activity_id
        from plan_snapshot_activities
        where snapshot_id = merge_base_id) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'none'
  from(
        select id as activity_id, name, metadata.tag_ids_activity_snapshot(id, merge_base_id),
               source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
               last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from plan_snapshot_activities psa
        where psa.snapshot_id = merge_base_id
        intersect
        select id as activity_id, name, metadata.tag_ids_activity_directive(id, plan_id_receiving),
               source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
               last_modified_arguments_at, metadata, anchor_id, anchored_to_start
        from activity_directive ad
        where ad.plan_id = plan_id_receiving) a;

  insert into receiving_diff (activity_id, change_type)
  select activity_id, 'modify'
  from (
        (select id as activity_id
         from plan_snapshot_activities
         where snapshot_id = merge_base_id
         union
         select id as activity_id
         from activity_directive
         where plan_id = plan_id_receiving)
        except
        select activity_id
        from receiving_diff) a;


  -- perform diff between s_diff and r_diff
      -- upload the non-conflicts into merge_staging_area
      -- upload conflict into conflicting_activities
  create temp table diff_diff(
    activity_id integer,
    change_type_supplying activity_change_type not null,
    change_type_receiving activity_change_type not null
  );

  -- this is going to require us to do the "none" operation again on the remaining modifies
  -- but otherwise we can just dump the 'adds' and 'none' into the merge staging area table

  -- 'delete' against a 'delete' does not enter the merge staging area table
  -- receiving 'delete' against supplying 'none' does not enter the merge staging area table

  insert into merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
         )
  -- 'adds' can go directly into the merge staging area table
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_snapshot(s_diff.activity_id, psa.snapshot_id),  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
    from supplying_diff as  s_diff
    join plan_snapshot_activities psa
      on s_diff.activity_id = psa.id
    where snapshot_id = snapshot_id_supplying and change_type = 'add'
  union
  -- an 'add' between the receiving plan and merge base is actually a 'none'
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_directive(r_diff.activity_id, ad.plan_id),  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'::activity_change_type
    from receiving_diff as r_diff
    join activity_directive ad
      on r_diff.activity_id = ad.id
    where plan_id = plan_id_receiving and change_type = 'add';

  -- put the rest in diff_diff
  insert into diff_diff (activity_id, change_type_supplying, change_type_receiving)
  select activity_id, supplying_diff.change_type as change_type_supplying, receiving_diff.change_type as change_type_receiving
    from receiving_diff
    join supplying_diff using (activity_id)
  where receiving_diff.change_type != 'add' or supplying_diff.change_type != 'add';

  -- ...except for that which is not recorded
  delete from diff_diff
    where (change_type_receiving = 'delete' and  change_type_supplying = 'delete')
       or (change_type_receiving = 'delete' and change_type_supplying = 'none');

  insert into merge_staging_area (
    merge_request_id, activity_id, name, tags, source_scheduling_goal_id, created_at,
    start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type
  )
  -- receiving 'none' and 'modify' against 'none' in the supplying side go into the merge staging area as 'none'
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_directive(diff_diff.activity_id, plan_id),  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'
    from diff_diff
    join activity_directive
      on activity_id=id
    where plan_id = plan_id_receiving
      and change_type_supplying = 'none'
      and (change_type_receiving = 'modify' or change_type_receiving = 'none')
  union
  -- supplying 'modify' against receiving 'none' go into the merge staging area as 'modify'
  select _merge_request_id, activity_id, name, metadata.tag_ids_activity_snapshot(diff_diff.activity_id, snapshot_id),  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join plan_snapshot_activities p
      on diff_diff.activity_id = p.id
    where snapshot_id = snapshot_id_supplying
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'modify')
  union
  -- supplying 'delete' against receiving 'none' go into the merge staging area as 'delete'
    select _merge_request_id, activity_id, name, metadata.tag_ids_activity_directive(diff_diff.activity_id, plan_id),  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, change_type_supplying
    from diff_diff
    join activity_directive p
      on diff_diff.activity_id = p.id
    where plan_id = plan_id_receiving
      and (change_type_receiving = 'none' and diff_diff.change_type_supplying = 'delete')
  union
  -- 'modify' against a 'modify' must be checked for equality first.
  select _merge_request_id, activity_id, name, tags,  source_scheduling_goal_id, created_at,
         start_offset, type, arguments, metadata, anchor_id, anchored_to_start, 'none'
  from (
    select activity_id, name, metadata.tag_ids_activity_directive(dd.activity_id, psa.snapshot_id) as tags,
           source_scheduling_goal_id, created_at, start_offset, type, arguments, metadata, anchor_id, anchored_to_start
      from plan_snapshot_activities psa
      join diff_diff dd
        on dd.activity_id = psa.id
      where psa.snapshot_id = snapshot_id_supplying
        and (dd.change_type_receiving = 'modify' and dd.change_type_supplying = 'modify')
    intersect
    select activity_id, name, metadata.tag_ids_activity_directive(dd.activity_id, ad.plan_id) as tags,
           source_scheduling_goal_id, created_at, start_offset, type, arguments, metadata, anchor_id, anchored_to_start
      from diff_diff dd
      join activity_directive ad
        on dd.activity_id = ad.id
      where ad.plan_id = plan_id_receiving
        and (dd.change_type_supplying = 'modify' and dd.change_type_receiving = 'modify')
  ) a;

  -- 'modify' against 'delete' and inequal 'modify' against 'modify' goes into conflict table (aka everything left in diff_diff)
  insert into conflicting_activities (merge_request_id, activity_id, change_type_supplying, change_type_receiving)
  select begin_merge._merge_request_id, activity_id, change_type_supplying, change_type_receiving
  from (select begin_merge._merge_request_id, activity_id
        from diff_diff
        except
        select msa.merge_request_id, activity_id
        from merge_staging_area msa) a
  join diff_diff using (activity_id);

  -- Fail if there are no differences between the snapshot and the plan getting merged
  validate_non_no_op_status := null;
  select change_type_receiving
  from conflicting_activities
  where merge_request_id = _merge_request_id
  limit 1
  into validate_non_no_op_status;

  if validate_non_no_op_status is null then
    select change_type
    from merge_staging_area msa
    where merge_request_id = _merge_request_id
    and msa.change_type != 'none'
    limit 1
    into validate_non_no_op_status;

    if validate_non_no_op_status is null then
      raise exception 'Cannot begin merge. The contents of the two plans are identical.';
    end if;
  end if;


  -- clean up
  drop table supplying_diff;
  drop table receiving_diff;
  drop table diff_diff;
end
$$;

-- CREATE MERGE REQUEST
drop function create_merge_request(plan_id_supplying integer, plan_id_receiving integer, requester integer);
create function create_merge_request(plan_id_supplying integer, plan_id_receiving integer, request_username text)
  returns integer
  language plpgsql as $$
declare
  merge_base_snapshot_id integer;
  validate_planIds integer;
  supplying_snapshot_id integer;
  merge_request_id integer;
begin
  if plan_id_receiving = plan_id_supplying then
    raise exception 'Cannot create a merge request between a plan and itself.';
  end if;
  select id from plan where plan.id = plan_id_receiving into validate_planIds;
  if validate_planIds is null then
    raise exception 'Plan receiving changes (Plan %) does not exist.', plan_id_receiving;
  end if;
  select id from plan where plan.id = plan_id_supplying into validate_planIds;
  if validate_planIds is null then
    raise exception 'Plan supplying changes (Plan %) does not exist.', plan_id_supplying;
  end if;

  select create_snapshot(plan_id_supplying) into supplying_snapshot_id;

  select get_merge_base(plan_id_receiving, supplying_snapshot_id) into merge_base_snapshot_id;
  if merge_base_snapshot_id is null then
    raise exception 'Cannot create merge request between unrelated plans.';
  end if;


  insert into merge_request(plan_id_receiving_changes, snapshot_id_supplying_changes, merge_base_snapshot_id, requester_username)
    values(plan_id_receiving, supplying_snapshot_id, merge_base_snapshot_id, request_username)
    returning id into merge_request_id;
  return merge_request_id;
end
$$;

-- DUPLICATE PLAN
comment on function duplicate_plan(plan_id integer, new_plan_name text, new_owner integer) is null;
drop function duplicate_plan(_plan_id integer, new_plan_name text, new_owner integer);
create function duplicate_plan(_plan_id integer, new_plan_name text, new_owner text)
  returns integer -- plan_id of the new plan
  security definer
  language plpgsql as $$
  declare
    validate_plan_id integer;
    new_plan_id integer;
    created_snapshot_id integer;
begin
  select id from plan where plan.id = _plan_id into validate_plan_id;
  if(validate_plan_id is null) then
    raise exception 'Plan % does not exist.', _plan_id;
  end if;

  select create_snapshot(_plan_id) into created_snapshot_id;

  insert into plan(revision, name, model_id, duration, start_time, parent_id, owner)
    select
        0, new_plan_name, model_id, duration, start_time, _plan_id, new_owner
    from plan where id = _plan_id
    returning id into new_plan_id;
  insert into activity_directive(
      id, plan_id, name, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start)
    select
      id, new_plan_id, name, source_scheduling_goal_id, created_at, last_modified_at, start_offset, type, arguments,
      last_modified_arguments_at, metadata, anchor_id, anchored_to_start
    from activity_directive where activity_directive.plan_id = _plan_id;

  with source_plan as (
    select simulation_template_id, arguments, simulation_start_time, simulation_end_time
    from simulation
    where simulation.plan_id = _plan_id
  )
  update simulation s
  set simulation_template_id = source_plan.simulation_template_id,
      arguments = source_plan.arguments,
      simulation_start_time = source_plan.simulation_start_time,
      simulation_end_time = source_plan.simulation_end_time
  from source_plan
  where s.plan_id = new_plan_id;

  insert into preset_to_directive(preset_id, activity_id, plan_id)
    select preset_id, activity_id, new_plan_id
    from preset_to_directive ptd where ptd.plan_id = _plan_id;

  insert into metadata.plan_tags(plan_id, tag_id)
    select new_plan_id, tag_id
    from metadata.plan_tags pt where pt.plan_id = _plan_id;
  insert into metadata.activity_directive_tags(plan_id, directive_id, tag_id)
    select new_plan_id, directive_id, tag_id
    from metadata.activity_directive_tags adt where adt.plan_id = _plan_id;

  insert into plan_latest_snapshot(plan_id, snapshot_id) values(new_plan_id, created_snapshot_id);
  return new_plan_id;
end
$$;
comment on function duplicate_plan(plan_id integer, new_plan_name text, new_owner text) is e''
  'Copies all of a given plan''s properties and activities into a new plan with the specified name.
  When duplicating a plan, a snapshot is created of the original plan.
  Additionally, that snapshot becomes the latest snapshot of the new plan.';

-- hasura_functions.begin_merge
create or replace function hasura_functions.begin_merge(merge_request_id integer, hasura_session json)
  returns hasura_functions.begin_merge_return_value -- plan_id of the new plan
  strict
  language plpgsql as $$
  declare
    non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[];
    conflicting_activities hasura_functions.get_conflicting_activities_return_value[];
    reviewer_username text;
begin
  reviewer_username := (hasura_session ->> 'x-hasura-user-id');
  call public.begin_merge($1, reviewer_username);

  non_conflicting_activities := array(select hasura_functions.get_non_conflicting_activities($1));
  conflicting_activities := array(select hasura_functions.get_conflicting_activities($1));

  return row($1, non_conflicting_activities, conflicting_activities)::hasura_functions.begin_merge_return_value;
end;
$$;

-- hasura_functions.create_merge_request
create or replace function hasura_functions.create_merge_request(source_plan_id integer, target_plan_id integer, hasura_session json)
  returns hasura_functions.create_merge_request_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
  requester_username text;
begin
  requester_username := (hasura_session ->> 'x-hasura-user-id');
  select create_merge_request(source_plan_id, target_plan_id, requester_username) into res;
  return row(res)::hasura_functions.create_merge_request_return_value;
end;
$$;

-- hasura_functions.duplicate_plan
create or replace function hasura_functions.duplicate_plan(plan_id integer, new_plan_name text, hasura_session json)
  returns hasura_functions.duplicate_plan_return_value -- plan_id of the new plan
  language plpgsql as $$
declare
  res integer;
  new_owner text;
begin
  new_owner := (hasura_session ->> 'x-hasura-user-id');
  select duplicate_plan(plan_id, new_plan_name, new_owner) into res;
  return row(res)::hasura_functions.duplicate_plan_return_value;
end;
$$;

--------------- TABLES ---------------
-- MERGE REQUEST
comment on column merge_request.requester is null;
comment on column merge_request.reviewer is null;

alter table public.merge_request
  add column requester_username text,
  add column reviewer_username text;

update public.merge_request mr
  set requester_username = u.username
  from metadata.users u where u.id = mr.requester;
update merge_request mr
  set reviewer_username = u.username
  from metadata.users u where u.id = mr.reviewer;

alter table public.merge_request
  alter column requester_username set not null,
  drop constraint merge_request_reviewer_exists,
  drop constraint merge_request_requester_exists,
  drop column requester,
  drop column reviewer;

comment on column merge_request.requester_username is e''
  'The username of the user who created this merge request.';
comment on column merge_request.reviewer_username is e''
  'The username of the user who reviews this merge request. Is empty until the request enters review.';

-- MERGE REQUEST COMMENT
comment on column merge_request_comment.commenter is null;

alter table public.merge_request_comment
  add column commenter_username text not null default '';

update public.merge_request_comment
  set commenter_username = u.username
  from metadata.users u
  where commenter = u.id;

alter table public.merge_request_comment
  drop constraint merge_request_commenter_exists,
  drop column commenter;

comment on column merge_request_comment.commenter_username is e''
  'The username of the user who left this comment.';

-- SIMULATION TEMPLATE
comment on column simulation_template.owner is null;

alter table public.simulation_template
  add column owner_name text not null default '';

update simulation_template
  set owner_name = u.username
  from metadata.users u
  where owner = u.id;

alter table public.simulation_template
  drop constraint simulation_template_owner_exists,
  drop column owner;
alter table public.simulation_template rename column owner_name to owner;

comment on column simulation_template.owner is e''
  'The user responsible for this simulation template';

-- SIMULATION_DATASET
comment on column simulation_dataset.requested_by is null;

alter table simulation_dataset
  add column requested_by_name text not null default '';

update simulation_dataset
  set requested_by_name = u.username
  from metadata.users u
  where requested_by = u.id;

alter table public.simulation_dataset
  drop constraint simulation_dataset_requested_by_exists,
  drop column requested_by;
alter table public.simulation_dataset rename column requested_by_name to requested_by;

comment on column simulation_dataset.requested_by is e''
  'The user who requested the simulation.';

-- PLAN COLLABORATORS
comment on column plan_collaborators.collaborator is null;

alter table public.plan_collaborators
  add column collaborator_name text,
  drop constraint plan_collaborators_pkey,
  add constraint plan_collaborators_pkey
    primary key (plan_id, collaborator);

update plan_collaborators
  set collaborator_name = u.username
  from metadata.users u
  where collaborator = u.id;

alter table public.plan_collaborators
  drop constraint plan_collaborator_collaborator_fkey,
  drop column collaborator;
alter table public.plan_collaborators rename column collaborator_name to collaborator;
delete from public.plan_collaborators
  where collaborator is null;
alter table public.plan_collaborators alter column collaborator set not null;

comment on column plan_collaborators.collaborator is e''
  'The username of the collaborator';

-- PLAN
comment on column plan.owner is null;
comment on column plan.updated_by is null;

alter table public.plan
  add column owner_name text not null default '',
  add column updated_by_name text not null default '';

update public.plan p
  set owner_name = u.username
  from metadata.users u where u.id = p.owner;
update public.plan p
  set updated_by_name = u.username
  from metadata.users u where u.id = p.updated_by;

alter table public.plan
  drop constraint plan_updated_by_exists,
  drop constraint plan_owner_exists,
  drop column updated_by,
  drop column owner;
alter table public.plan rename column owner_name to owner;
alter table public.plan rename column updated_by_name to updated_by;

comment on column plan.owner is e''
  'The user who owns the plan.';
comment on column plan.updated_by is e''
  'The user who last updated the plan.';

-- MISSION MODEL
comment on column mission_model.owner is null;

alter table public.mission_model
  add column owner_name text;

update mission_model
  set owner_name = u.username
  from metadata.users u
  where owner = u.id;

alter table public.mission_model
  drop constraint mission_model_owner_exists,
  drop column owner;
alter table public.mission_model rename column owner_name to owner;

comment on column mission_model.owner is e''
  'A human-meaningful identifier for the user responsible for this model.';

-- CONSTRAINTS
comment on column "constraint".owner is null;
comment on column "constraint".updated_by is null;

alter table public."constraint"
  add column owner_name text not null default '',
  add column updated_by_name text;

update public."constraint" c
  set owner_name = u.username
  from metadata.users u where u.id = c.owner;
update public."constraint" c
  set updated_by_name = u.username
  from metadata.users u where u.id = c.updated_by;

alter table public."constraint"
  drop constraint constraint_updated_by_exists,
  drop constraint constraint_owner_exists,
  drop column updated_by,
  drop column owner;
alter table public."constraint" rename column owner_name to owner;
alter table public."constraint" rename column updated_by_name to updated_by;

comment on column "constraint".owner is e''
  'The user responsible for this constraint.';
comment on column "constraint".updated_by is e''
  'The user who last modified this constraint.';

-- ACTIVITY PRESETS
comment on column activity_presets.owner is null;

alter table public.activity_presets
  add column owner_name text not null default '';

update public.activity_presets t
  set owner_name = u.username
  from metadata.users u
  where u.id = t.owner;

alter table public.activity_presets
  drop constraint activity_presets_owner_exists,
  drop column owner;
alter table public.activity_presets
  rename column owner_name to owner;

comment on column activity_presets.owner is e''
  'The owner of this activity preset';

-- TAGS
comment on column metadata.tags.owner is null;

alter table metadata.tags
  add column owner_name text;

update metadata.tags t
  set owner_name = u.username
  from metadata.users u
  where u.id = t.owner;
update metadata.tags
  set owner_name = ''
  where owner_name is null;

alter table metadata.tags
  drop constraint tags_owner_exists,
  drop column owner;
alter table metadata.tags
  rename owner_name to owner;
alter table metadata.tags
  alter column owner set not null;

comment on column metadata.tags.owner is e''
  'The user responsible for this tag. '
  '''Mission Model'' is used to represent tags originating from an uploaded mission model'
  '''Aerie Legacy'' is used to represent tags originating from a version of Aerie prior to this table''s creation.';

-- USERS AND ROLES VIEW
comment on view metadata.users_and_roles is null;
drop view metadata.users_and_roles;

-- USERS ALLOWED ROLES
comment on table metadata.users_allowed_roles is null;
drop table metadata.users_allowed_roles;

-- USERS
comment on column metadata.users.default_role is null;
comment on column metadata.users.username is null;
comment on column metadata.users.id is null;
comment on table metadata.users is null;
drop table metadata.users;

-- USER ROLES
comment on table metadata.user_roles is null;
drop table metadata.user_roles;

call migrations.mark_migration_rolled_back('18');
