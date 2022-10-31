create type merge_request_status as enum ('pending', 'in-progress','accepted', 'rejected', 'withdrawn');

create table merge_request(
      id integer generated always as identity
        primary key,
      plan_id_receiving_changes integer,
      snapshot_id_supplying_changes integer,
      merge_base_snapshot_id integer not null,
      status merge_request_status default 'pending',
      requester_username text not null,
      reviewer_username text
);

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
