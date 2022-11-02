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

comment on table merge_request is e''
  'A request to merge the state of the activities from one plan onto another.';

comment on column merge_request.id is e''
  'The synthetic identifier for this merge request.';
comment on column merge_request.plan_id_receiving_changes is e''
  'The plan id of the plan to receive changes as a result of this merge request being processed and committed.'
  '\nAlso known as "Target".';
comment on column merge_request.snapshot_id_supplying_changes is e''
  'The snapshot id used to supply changes when this merge request is processed.'
  '\nAlso known as "Source".';
comment on column merge_request.merge_base_snapshot_id is e''
  'The snapshot id that is the nearest common ancestor between the '
  'plan_id_receiving_changes and the snapshot_id_supplying_changes of this merge request.';
comment on column merge_request.status is e''
  'The current status of this merge request.';
comment on column merge_request.requester_username is e''
  'The username of the user who created this merge request.';
comment on column merge_request.reviewer_username is e''
  'The username of the user who reviews this merge request. Is empty until the request enters review.';

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
