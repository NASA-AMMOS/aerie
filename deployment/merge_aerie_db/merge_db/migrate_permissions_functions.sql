create or replace function permissions.insert_permission_for_user_role()
  returns trigger
  security definer
language plpgsql as $$
  begin
    insert into permissions.user_role_permission(role)
    values (new.role);
    return new;
  end
$$;

create or replace function permissions.validate_permissions_json()
returns trigger
language plpgsql as $$
  declare
    error_msg text;
    plan_merge_fns text[];
begin
  error_msg = '';

  plan_merge_fns := '{
    "begin_merge",
    "cancel_merge",
    "commit_merge",
    "create_merge_rq",
    "deny_merge",
    "get_conflicting_activities",
    "get_non_conflicting_activities",
    "set_resolution",
    "set_resolution_bulk",
    "withdraw_merge_rq"
    }';

  -- Do all the validation checks up front
  -- Duplicate keys are not checked for, as as all but the last instance is removed
  -- during conversion of JSON Text to JSONB (https://www.postgresql.org/docs/14/datatype-json.html)
  create temp table _validate_functions_table as
  select
    jsonb_object_keys(new.function_permissions) as function_key,
    new.function_permissions ->> jsonb_object_keys(new.function_permissions) as function_permission,
    jsonb_object_keys(new.function_permissions) = any(enum_range(null::permissions.function_permission_key)::text[]) as valid_function_key,
    new.function_permissions ->> jsonb_object_keys(new.function_permissions) = any(enum_range(null::permissions.permission)::text[]) as valid_function_permission,
    jsonb_object_keys(new.function_permissions) = any(plan_merge_fns) as is_plan_merge_key,
  	new.function_permissions ->> jsonb_object_keys(new.function_permissions) = any(enum_range('PLAN_OWNER_SOURCE'::permissions.permission, 'PLAN_OWNER_COLLABORATOR_TARGET'::permissions.permission)::text[]) as is_plan_merge_permission;

  create temp table _validate_actions_table as
  select
    jsonb_object_keys(new.action_permissions) as action_key,
    new.action_permissions ->> jsonb_object_keys(new.action_permissions) as action_permission,
    jsonb_object_keys(new.action_permissions) = any(enum_range(null::permissions.action_permission_key)::text[]) as valid_action_key,
    new.action_permissions ->> jsonb_object_keys(new.action_permissions) = any(enum_range(null::permissions.permission)::text[]) as valid_action_permission,
  	new.action_permissions ->> jsonb_object_keys(new.action_permissions) = any(enum_range('PLAN_OWNER_SOURCE'::permissions.permission, 'PLAN_OWNER_COLLABORATOR_TARGET'::permissions.permission)::text[]) as is_plan_merge_permission;


  -- Get any invalid Action Keys
  if exists(select from _validate_actions_table where not valid_action_key)
  then
    error_msg = 'The following action keys are not valid: '
                 || (select string_agg(action_key, ', ')
                     from _validate_actions_table
                     where not valid_action_key)
                 ||e'\n';
  end if;
  -- Get any invalid Function Keys
  if exists(select from _validate_functions_table where not valid_function_key)
  then
    error_msg = error_msg
                 || 'The following function keys are not valid: '
                 || (select string_agg(function_key, ', ')
                     from _validate_functions_table
                     where not valid_function_key);
  end if;

  -- Raise if there were invalid Action/Function Keys
  if error_msg != '' then
    raise exception using
      message = 'invalid keys in supplied row',
      detail = trim(both e'\n' from error_msg),
      errcode = 'invalid_json_text',
      hint = 'Visit https://nasa-ammos.github.io/aerie-docs/deployment/advanced-permissions/#action-and-function-permissions for a list of valid keys.';
  end if;

  -- Get any values that aren't Action Permissions
  if exists(select from _validate_actions_table where not valid_action_permission)
  then
    error_msg = 'The following action keys have invalid permissions: {'
                || (select string_agg(action_key || ': ' || action_permission, ', ')
                    from _validate_actions_table
                    where not valid_action_permission)
                ||e'}\n';
  end if;

  -- Get any values that aren't Function Permissions
  if exists(select from _validate_functions_table where not valid_function_permission)
  then
    error_msg = error_msg
                || 'The following function keys have invalid permissions: {'
                || (select string_agg(function_key || ': ' || function_permission, ', ')
                    from _validate_functions_table
                    where not valid_function_permission)
                || '}';
  end if;

  -- Raise if there were invalid Action/Function Permissions
  if error_msg != '' then
    raise exception using
      message = 'invalid permissions in supplied row',
      detail = trim(both e'\n' from error_msg),
      errcode = 'invalid_json_text',
      hint = 'Visit https://nasa-ammos.github.io/aerie-docs/deployment/advanced-permissions/#action-and-function-permissions for a list of valid Permissions.';
  end if;

	-- Check that no Actions have Plan Merge Permissions
  if exists(select from _validate_actions_table where is_plan_merge_permission)
  then
    error_msg = 'The following action keys may not take plan merge permissions: {'
                || (select string_agg(action_key || ': ' || action_permission, ', ')
                    from _validate_actions_table
                    where is_plan_merge_permission)
                ||e'}\n';
  end if;

  -- Check that no non-Plan Merge Functions have Plan Merge Permissions
  if exists(select from _validate_functions_table where is_plan_merge_permission and not is_plan_merge_key)
  then
    error_msg = error_msg
                || 'The following function keys may not take plan merge permissions: {'
                || (select string_agg(function_key || ': ' || function_permission, ', ')
                    from _validate_functions_table
                    where is_plan_merge_permission and not is_plan_merge_key)
                  || '}';
  end if;

  -- Raise if Plan Merge Permissions were improperly applied
  if error_msg != '' then
    raise exception using
      message = 'invalid permissions in supplied row',
      detail = trim(both e'\n' from error_msg),
      errcode = 'invalid_json_text',
      hint = 'Visit https://nasa-ammos.github.io/aerie-docs/deployment/advanced-permissions/#action-and-function-permissions for more information.';
  end if;

  -- Drop Temp Tables
  drop table _validate_functions_table;
  drop table _validate_actions_table;

  return new;
end
$$;

create or replace procedure permissions.check_general_permissions(
  _function permissions.function_permission_key,
  _permission permissions.permission,
  _plan_id integer,
  _user text)
language plpgsql as $$
declare
  _mission_model_id integer;
  _plan_name text;
begin
  select name from merlin.plan where id = _plan_id into _plan_name;

  -- MISSION_MODEL_OWNER: The user must own the relevant Mission Model
  if _permission = 'MISSION_MODEL_OWNER' then
    select id from merlin.mission_model mm
    where mm.id = (select model_id from merlin.plan p where p.id = _plan_id)
    into _mission_model_id;

    if not exists(select * from merlin.mission_model mm where mm.id = _mission_model_id and mm.owner =_user) then
        raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not MISSION_MODEL_OWNER on Model ' || _mission_model_id ||'.';
    end if;

  -- OWNER: The user must be the owner of all relevant objects directly used by the KEY
  -- In most cases, OWNER is equivalent to PLAN_OWNER. Use a custom solution when that is not true.
  elseif _permission = 'OWNER' then
		if not exists(select * from merlin.plan p where p.id = _plan_id and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not OWNER on Plan ' || _plan_id ||' ('|| _plan_name ||').';
    end if;

  -- PLAN_OWNER: The user must be the Owner of the relevant Plan
  elseif _permission = 'PLAN_OWNER' then
    if not exists(select * from merlin.plan p where p.id = _plan_id and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER on Plan '|| _plan_id ||' ('|| _plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR:	The user must be a Collaborator of the relevant Plan. The Plan Owner is NOT considered a Collaborator of the Plan
  elseif _permission = 'PLAN_COLLABORATOR' then
    if not exists(select * from merlin.plan_collaborators pc where pc.plan_id = _plan_id and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_COLLABORATOR on Plan '|| _plan_id ||' ('|| _plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR:	The user must be either the Owner or a Collaborator of the relevant Plan
  elseif _permission = 'PLAN_OWNER_COLLABORATOR' then
    if not exists(select * from merlin.plan p where p.id = _plan_id and p.owner = _user) then
      if not exists(select * from merlin.plan_collaborators pc where pc.plan_id = _plan_id and pc.collaborator = _user) then
        raise insufficient_privilege
          using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER_COLLABORATOR on Plan '|| _plan_id ||' ('|| _plan_name ||').';
      end if;
    end if;
  end if;
end
$$;

create or replace function permissions.get_function_permissions(_function permissions.function_permission_key, hasura_session json)
returns permissions.permission
stable
language plpgsql as $$
declare
  _role text;
  _function_permission permissions.permission;
begin
  _role := permissions.get_role(hasura_session);
  -- The aerie_admin role is always treated as having NO_CHECK permissions on all functions.
  if _role = 'aerie_admin' then return 'NO_CHECK'; end if;

  select (function_permissions ->> _function::text)::permissions.permission
  from permissions.user_role_permission urp
  where urp.role = _role
  into _function_permission;

  -- The absence of the function key means that the role does not have permission to perform the function.
  if _function_permission is null then
    raise insufficient_privilege
      using message = 'User with role '''|| _role ||''' is not permitted to run '''|| _function ||'''';
  end if;

  return _function_permission::permissions.permission;
end
$$;

create or replace function permissions.get_role(hasura_session json)
returns text
stable
language plpgsql as $$
declare
  _role text;
  _username text;
begin
  _role := hasura_session ->> 'x-hasura-role';
  if _role is not null then
    return _role;
  end if;
  _username := hasura_session ->> 'x-hasura-user-id';
  select default_role from permissions.users u
  where u.username = _username into _role;
  if _role is null then
    raise exception 'Invalid username: %', _username;
  end if;
  return _role;
end
$$;

create or replace function permissions.raise_if_plan_merge_permission(_function permissions.function_permission_key, _permission permissions.permission)
returns void
immutable
language plpgsql as $$
begin
  if _permission::text = any(array['PLAN_OWNER_SOURCE', 'PLAN_COLLABORATOR_SOURCE', 'PLAN_OWNER_COLLABORATOR_SOURCE',
    'PLAN_OWNER_TARGET', 'PLAN_COLLABORATOR_TARGET', 'PLAN_OWNER_COLLABORATOR_TARGET'])
  then
    raise 'Invalid Permission: The Permission ''%'' may not be applied to function ''%''', _permission, _function;
  end if;
end
$$;

create or replace procedure permissions.check_merge_permissions(_function permissions.function_permission_key, _merge_request_id integer, hasura_session json)
language plpgsql as $$
declare
  _plan_id_receiving_changes integer;
  _plan_id_supplying_changes integer;
  _function_permission permissions.permission;
  _user text;
begin
  select plan_id_receiving_changes
  from merlin.merge_request mr
  where mr.id = _merge_request_id
  into _plan_id_receiving_changes;

  select plan_id
  from merlin.plan_snapshot ps, merlin.merge_request mr
  where mr.id = _merge_request_id and ps.snapshot_id = mr.snapshot_id_supplying_changes
  into _plan_id_supplying_changes;

  _user := (hasura_session ->> 'x-hasura-user-id');
  _function_permission := permissions.get_function_permissions('get_non_conflicting_activities', hasura_session);
  call permissions.check_merge_permissions(_function, _function_permission, _plan_id_receiving_changes,
    _plan_id_supplying_changes, _user);
end
$$;

create or replace procedure permissions.check_merge_permissions(
  _function permissions.function_permission_key,
  _permission permissions.permission,
  _plan_id_receiving integer,
  _plan_id_supplying integer,
  _user text)
language plpgsql as $$
declare
  _supplying_plan_name text;
  _receiving_plan_name text;
begin
  select name from merlin.plan where id = _plan_id_supplying into _supplying_plan_name;
  select name from merlin.plan where id = _plan_id_receiving into _receiving_plan_name;

  -- MISSION_MODEL_OWNER: The user must own the relevant Mission Model
  if _permission = 'MISSION_MODEL_OWNER' then
    call permissions.check_general_permissions(_function, _permission, _plan_id_receiving, _user);

  -- OWNER: The user must be the Owner of both Plans
  elseif _permission = 'OWNER' then
    if not (exists(select * from merlin.plan p where p.id = _plan_id_receiving and p.owner = _user)) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not OWNER on Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||').';
    elseif not (exists(select * from merlin.plan p2 where p2.id = _plan_id_supplying and p2.owner = _user)) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not OWNER on Plan '|| _plan_id_supplying
                          ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_OWNER: The user must be the Owner of either Plan
  elseif _permission = 'PLAN_OWNER' then
    if not exists(select *
                  from merlin.plan p
                  where (p.id = _plan_id_receiving or p.id = _plan_id_supplying)
                    and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not PLAN_OWNER on either Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||') or Plan '|| _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR:	The user must be a Collaborator of either Plan. The Plan Owner is NOT considered a Collaborator of the Plan
  elseif _permission = 'PLAN_COLLABORATOR' then
    if not exists(select *
                  from merlin.plan_collaborators pc
                  where (pc.plan_id = _plan_id_receiving or pc.plan_id = _plan_id_supplying)
                    and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not PLAN_COLLABORATOR on either Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||') or Plan '|| _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR:	The user must be either the Owner or a Collaborator of either Plan
  elseif _permission = 'PLAN_OWNER_COLLABORATOR' then
    if not exists(select *
                  from merlin.plan p
                  where (p.id = _plan_id_receiving or p.id = _plan_id_supplying)
                    and p.owner = _user) then
      if not exists(select *
                    from merlin.plan_collaborators pc
                    where (pc.plan_id = _plan_id_receiving or pc.plan_id = _plan_id_supplying)
                      and pc.collaborator = _user) then
        raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''
                          || _user ||''' is not PLAN_OWNER_COLLABORATOR on either Plan '|| _plan_id_receiving
                          ||' ('|| _receiving_plan_name ||') or Plan '|| _plan_id_supplying ||' ('|| _supplying_plan_name ||').';

      end if;
    end if;

  -- PLAN_OWNER_SOURCE:	The user must be the Owner of the Supplying Plan
  elseif _permission = 'PLAN_OWNER_SOURCE' then
    if not exists(select *
                  from merlin.plan p
                  where p.id = _plan_id_supplying and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER on Source Plan '
                          || _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR_SOURCE: The user must be a Collaborator of the Supplying Plan.
  elseif _permission = 'PLAN_COLLABORATOR_SOURCE' then
    if not exists(select *
                  from merlin.plan_collaborators pc
                  where pc.plan_id = _plan_id_supplying and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_COLLABORATOR on Source Plan '
                          || _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR_SOURCE:	The user must be either the Owner or a Collaborator of the Supplying Plan.
  elseif _permission = 'PLAN_OWNER_COLLABORATOR_SOURCE' then
    if not exists(select *
                  from merlin.plan p
                  where p.id = _plan_id_supplying and p.owner = _user) then
      if not exists(select *
                    from merlin.plan_collaborators pc
                    where pc.plan_id = _plan_id_supplying and pc.collaborator = _user) then
        raise insufficient_privilege
          using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER_COLLABORATOR on Source Plan '
                          || _plan_id_supplying ||' ('|| _supplying_plan_name ||').';
      end if;
    end if;

  -- PLAN_OWNER_TARGET: The user must be the Owner of the Receiving Plan.
  elseif _permission = 'PLAN_OWNER_TARGET' then
    if not exists(select *
                  from merlin.plan p
                  where p.id = _plan_id_receiving and p.owner = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER on Target Plan '
                          || _plan_id_receiving ||' ('|| _receiving_plan_name ||').';
    end if;

  -- PLAN_COLLABORATOR_TARGET: The user must be a Collaborator of the Receiving Plan.
  elseif _permission = 'PLAN_COLLABORATOR_TARGET' then
    if not exists(select *
                  from merlin.plan_collaborators pc
                  where pc.plan_id = _plan_id_receiving and pc.collaborator = _user) then
      raise insufficient_privilege
        using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_COLLABORATOR on Target Plan '
                          || _plan_id_receiving ||' ('|| _receiving_plan_name ||').';
    end if;

  -- PLAN_OWNER_COLLABORATOR_TARGET: The user must be either the Owner or a Collaborator of the Receiving Plan.
  elseif _permission = 'PLAN_OWNER_COLLABORATOR_TARGET' then
    if not exists(select *
                  from merlin.plan p
                  where p.id = _plan_id_receiving and p.owner = _user) then
      if not exists(select *
                    from merlin.plan_collaborators pc
                    where pc.plan_id = _plan_id_receiving and pc.collaborator = _user) then
        raise insufficient_privilege
          using message = 'Cannot run '''|| _function ||''': '''|| _user ||''' is not PLAN_OWNER_COLLABORATOR on Target Plan '
                          || _plan_id_receiving ||' ('|| _receiving_plan_name ||').';
      end if;
    end if;
  end if;
end
$$;
