-- Activity Presets
create function hasura_functions.apply_preset_to_activity(_preset_id int, _activity_id int, _plan_id int, hasura_session json)
returns activity_directive
strict
volatile
language plpgsql as $$
  declare
    returning_directive activity_directive;
    ad_activity_type text;
    preset_activity_type text;
    _function_permission metadata.permission;
    _user text;
begin
    _function_permission := metadata.get_function_permissions('apply_preset', hasura_session);
    perform metadata.raise_if_plan_merge_permission('apply_preset', _function_permission);
    -- Check valid permissions
    _user := hasura_session ->> 'x-hasura-user-id';
    if not _function_permission = 'NO_CHECK' then
      if _function_permission = 'OWNER' then
        if not exists(select * from public.activity_presets ap where ap.id = _preset_id and ap.owner = _user) then
          raise insufficient_privilege
            using message = 'Cannot run ''apply_preset'': '''|| _user ||''' is not OWNER on Activity Preset '
                            || _preset_id ||'.';
        end if;
      end if;
      -- Additionally, the user needs to be OWNER of the plan
      call metadata.check_general_permissions('apply_preset', _function_permission, _plan_id, _user);
    end if;

    if not exists(select id from public.activity_directive where (id, plan_id) = (_activity_id, _plan_id)) then
      raise exception 'Activity directive % does not exist in plan %', _activity_id, _plan_id;
    end if;
    if not exists(select id from public.activity_presets where id = _preset_id) then
      raise exception 'Activity preset % does not exist', _preset_id;
    end if;

    select type from activity_directive where (id, plan_id) = (_activity_id, _plan_id) into ad_activity_type;
    select associated_activity_type from activity_presets where id = _preset_id into preset_activity_type;

    if (ad_activity_type != preset_activity_type) then
      raise exception 'Cannot apply preset for activity type "%" onto an activity of type "%".', preset_activity_type, ad_activity_type;
    end if;

    update activity_directive
    set arguments = (select arguments from activity_presets where id = _preset_id)
    where (id, plan_id) = (_activity_id, _plan_id);

    insert into preset_to_directive(preset_id, activity_id, plan_id)
    select _preset_id, _activity_id, _plan_id
    on conflict (activity_id, plan_id) do update
    set preset_id = _preset_id;

    select * from activity_directive
    where (id, plan_id) = (_activity_id, _plan_id)
    into returning_directive;

    return returning_directive;
end
$$;
