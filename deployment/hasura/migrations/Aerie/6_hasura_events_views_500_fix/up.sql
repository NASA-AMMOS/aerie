create or replace function hasura.get_event_logs(_trigger_name text)
returns table (
  model_id int,
  model_name text,
  model_version text,
  triggering_user text,
  delivered boolean,
  success boolean,
  tries int,
  created_at timestamp,
  next_retry_at timestamp,
  status int,
  error jsonb,
  error_message text,
  error_type text
)
stable
security invoker
language plpgsql as $$
begin
  return query (
    select
      (el.payload->'data'->'new'->>'id')::int as model_id,
      el.payload->'data'->'new'->>'name' as model_name,
      el.payload->'data'->'new'->>'version' as model_version,
      el.payload->'session_variables'->>'x-hasura-user-id' as triggering_user,
      el.delivered,
      eil.status is not distinct from 200 as success, -- is not distinct from to catch `null`
      el.tries,
      el.created_at,
      el.next_retry_at,
      eil.status,
      eil.response -> 'data'-> 'message' as error,
      eil.response -> 'data'-> 'message'->>'message' as error_message,
      eil.response -> 'data'-> 'message'->>'type' as error_type
      from hdb_catalog.event_log el
      join hdb_catalog.event_invocation_logs eil on el.id = eil.event_id
      where trigger_name = _trigger_name);
end;
$$;
call migrations.mark_migration_applied('6');
