create type merge_request_status as enum ('pending', 'in-progress','accepted', 'rejected', 'withdrawn');

create table merge_request(
      id integer generated always as identity,
      plan_id_receiving_changes integer,
      snapshot_id_supplying_changes integer,
      status merge_request_status default 'pending',
      constraint request_artificial_key
        primary key (id)
);

create or replace function create_merge_request(plan_id_supplying integer, plan_id_receiving integer)
  returns integer
  language plpgsql as $$
declare
  validate_related integer;
  validate_planIds integer;
  supplying_snapshot_id integer;
  merge_request_id integer;
begin
  select id from plan where plan.id = plan_id_receiving into validate_planIds;
  if validate_planIds is null then
    raise exception 'Plan receiving changes (Plan %) does not exist.', plan_id_receiving;
  end if;
  select id from plan where plan.id = plan_id_supplying into validate_planIds;
  if validate_planIds is null then
    raise exception 'Plan supplying changes (Plan %) does not exist.', plan_id_supplying;
  end if;

  select create_snapshot(plan_id_supplying) into supplying_snapshot_id;

  select get_merge_base(plan_id_receiving, supplying_snapshot_id) into validate_related;
  if validate_related is null then
    raise exception 'Cannot create merge request between unrelated plans.';
  end if;


  insert into merge_request(plan_id_receiving_changes, snapshot_id_supplying_changes)
    values(plan_id_receiving, supplying_snapshot_id)
    returning id into merge_request_id;
  return merge_request_id;
end
$$;

create or replace procedure withdraw_merge_request(request_id integer)
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
