create procedure merlin.plan_locked_exception(plan_id integer)
language plpgsql as $$
  begin
    if(select is_locked from merlin.plan p where p.id = plan_id limit 1) then
      raise exception 'Plan % is locked.', plan_id;
    end if;
  end
$$;

comment on procedure merlin.plan_locked_exception(plan_id integer) is e''
  'Verify that the specified plan is unlocked, throwing an exception if not.';
