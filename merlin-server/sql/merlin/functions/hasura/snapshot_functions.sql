create table hasura_functions.create_snapshot_return_value(snapshot_id integer);
create function hasura_functions.create_snapshot(_plan_id integer, _snapshot_name text, hasura_session json)
  returns hasura_functions.create_snapshot_return_value
  volatile
  language plpgsql as $$
declare
  _snapshot_id integer;
  _snapshotter text;
  _function_permission metadata.permission;
begin
  _snapshotter := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := metadata.get_function_permissions('create_snapshot', hasura_session);
  perform metadata.raise_if_plan_merge_permission('create_snapshot', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('create_snapshot', _function_permission, _plan_id, _snapshotter);
  end if;
  if _snapshot_name is null then
    raise exception 'Snapshot name cannot be null.';
  end if;

  select create_snapshot(_plan_id, _snapshot_name, _snapshotter) into _snapshot_id;
  return row(_snapshot_id)::hasura_functions.create_snapshot_return_value;
end;
$$;

create function hasura_functions.restore_from_snapshot(_plan_id integer, _snapshot_id integer, hasura_session json)
	returns hasura_functions.create_snapshot_return_value
	volatile
	language plpgsql as $$
declare
  _user text;
  _function_permission metadata.permission;
begin
	_user := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := metadata.get_function_permissions('restore_snapshot', hasura_session);
  perform metadata.raise_if_plan_merge_permission('restore_snapshot', _function_permission);
  if not _function_permission = 'NO_CHECK' then
    call metadata.check_general_permissions('restore_snapshot', _function_permission, _plan_id, _user);
  end if;

  call restore_from_snapshot(_plan_id, _snapshot_id);
  return row(_snapshot_id)::hasura_functions.create_snapshot_return_value;
end
$$;
