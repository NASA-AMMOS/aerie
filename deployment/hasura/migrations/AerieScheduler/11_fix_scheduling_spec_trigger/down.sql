create or replace function increment_revision_on_goal_update()
  returns trigger
  security definer
  language plpgsql as $$begin
  with goals as (
    select g.specification_id from scheduling_specification_goals as g
    where g.specification_id = new.id
  )
  update scheduling_specification set revision = revision + 1
  where exists(select 1 from goals where specification_id = id);
return new;
end$$;

call migrations.mark_migration_rolled_back('11');
