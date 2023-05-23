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

create procedure withdraw_merge_request(request_id integer)
  language plpgsql as
$$
declare
  validate_status merge_request_status;
begin
  select status from merge_request where id = request_id into validate_status;
  if validate_status is null then
    raise exception 'Merge request % does not exist. Cannot withdraw request.', request_id;
  elsif validate_status != 'pending' and validate_status != 'withdrawn' then
    raise exception 'Cannot withdraw request.';
  end if;

  update merge_request
    set status = 'withdrawn'
    where id = request_id;
end
$$;

/*
  - Discard everything that was in the staging area
  - Then, unlock the to-be-edited plan
  - Then, change the merge request's status to 'rejected'
*/
create procedure deny_merge(request_id integer)
  language plpgsql as $$
  begin
    if(select id from merge_request where id = request_id) is null then
      raise exception 'Invalid merge request id %.', request_id;
    end if;

    if (select status from merge_request where id = request_id) != 'in-progress' then
      raise exception 'Cannot reject merge not in progress.';
    end if;

    delete from conflicting_activities where merge_request_id = request_id;
    delete from merge_staging_area where merge_staging_area.merge_request_id = deny_merge.request_id;

    update merge_request
    set status = 'rejected'
    where merge_request.id = request_id;

    update plan
    set is_locked = false
    where plan.id = (select plan_id_receiving_changes from merge_request where id = request_id);
  end
  $$;

/*
  - Discard everything that was in the staging area
  - Then, unlock the to-be-edited plan
  - Then, change the merge request's status to 'pending'
*/
create procedure cancel_merge(request_id integer)
  language plpgsql as $$
  declare
    verify_status merge_request_status;
begin
    if(select id from merge_request where id = request_id) is null then
      raise exception 'Invalid merge request id %.', request_id;
    end if;

    select status from merge_request where id = request_id into verify_status;
    if not (verify_status = 'in-progress' or verify_status = 'pending') then
      raise exception 'Cannot cancel merge.';
    end if;

    delete from conflicting_activities where merge_request_id = request_id;
    delete from merge_staging_area where merge_staging_area.merge_request_id = cancel_merge.request_id;

    update merge_request
    set status = 'pending'
    where merge_request.id = request_id;

    update plan
    set is_locked = false
    where plan.id = (select plan_id_receiving_changes from merge_request where id = request_id);
end
$$;
