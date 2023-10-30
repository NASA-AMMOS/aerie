create table activity_directive_changelog (
  revision integer not null,
  plan_id integer not null,
  activity_directive_id integer not null,

  name text,
  source_scheduling_goal_id integer,
  changed_at timestamptz not null default now(),
  changed_by text,
  start_offset interval not null,
  type text not null,
  arguments merlin_argument_set not null,
  changed_arguments_at timestamptz not null default now(),
  metadata merlin_activity_directive_metadata_set default '{}'::jsonb,

  anchor_id integer default null,
  anchored_to_start boolean default true not null,

  validation_results jsonb default '{}'::jsonb,

  constraint activity_directive_changelog_natural_key
    primary key (plan_id, activity_directive_id, revision),
  constraint changelog_references_activity_directive
    foreign key (activity_directive_id, plan_id)
    references activity_directive
    on update cascade
    on delete cascade,
  constraint changed_by_exists
    foreign key (changed_by)
    references metadata.users
    on update cascade
    on delete set null
);

comment on table activity_directive_changelog is e''
  'A changelog that captures the 10 most recent revisions for each activity directive\n'
  'See activity_directive comments for descriptions of shared fields';

create function store_activity_directive_change()
  returns trigger
  language plpgsql as $$
begin
  insert into activity_directive_changelog (
    revision,
    plan_id,
    activity_directive_id,
    name,
    start_offset,
    type,
    arguments,
    changed_arguments_at,
    metadata,
    changed_by,
    anchor_id,
    anchored_to_start)
  values (
    (select coalesce(max(revision), -1) + 1
     from activity_directive_changelog
     where plan_id = new.plan_id
      and activity_directive_id = new.id),
    new.plan_id,
    new.id,
    new.name,
    new.start_offset,
    new.type,
    new.arguments,
    new.last_modified_arguments_at,
    new.metadata,
    new.last_modified_by,
    new.anchor_id,
    new.anchored_to_start);

  return new;
end
$$;

create trigger store_activity_directive_change_trigger
  after update or insert on activity_directive
  for each row
  execute function store_activity_directive_change();

create function delete_min_activity_directive_revision()
  returns trigger
  language plpgsql as $$
begin
  delete from activity_directive_changelog
  where activity_directive_id = new.activity_directive_id
    and plan_id = new.plan_id
    and revision = (select min(revision)
                    from activity_directive_changelog
                    where activity_directive_id = new.activity_directive_id
                      and plan_id = new.plan_id);
  return new;
end$$;

create trigger delete_min_activity_directive_revision_trigger
  after insert on activity_directive_changelog
  for each row
  when (new.revision > 10)
  execute function delete_min_activity_directive_revision();

create or replace function notify_validation_workers()
  returns trigger
security definer
  language plpgsql as $$
begin
    perform (
      with payload(activity_directive_id,
                   plan_id,
                   revision,
                   plan_id,
                   model_id,
                   type,
                   arguments) as
             (
               select new.activity_directive_id,
                      NEW.plan_id,
                      NEW.revision,
                      NEW.plan_id,
                      (select model_id from plan where id = NEW.plan_id),
                      NEW.type,
                      NEW.arguments
             )
      select pg_notify('validation_notification', json_strip_nulls(row_to_json(payload))::text)
      from payload
    );
    return null;
end$$;

create trigger notify_validation_workers_trigger
  after insert on activity_directive_changelog
  for each row
  execute function notify_validation_workers();
