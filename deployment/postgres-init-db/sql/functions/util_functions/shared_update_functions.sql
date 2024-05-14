create function util_functions.set_updated_at()
returns trigger
security invoker
language plpgsql as $$begin
  new.updated_at = now();
  return new;
end$$;

create function util_functions.increment_revision_update()
returns trigger
security invoker
language plpgsql as $$
begin
  new.revision = old.revision +1;
  return new;
end$$;

create function util_functions.raise_duration_is_negative()
returns trigger
security invoker
language plpgsql as $$begin
  raise exception 'invalid duration, expected nonnegative duration but found: %', new.duration;
end$$;
