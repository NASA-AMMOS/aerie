create function permissions.get_role(hasura_session json)
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
