create table ui.seen_sources
(
    plan_id integer not null,
    derivation_group_name text not null,
    last_acknowledged_at timestamp with time zone default now() not null,

    constraint seen_sources_pkey
      primary key (plan_id, derivation_group_name),
    constraint seen_sources_references_plan_derivation_group
      foreign key (plan_id, derivation_group_name)
      references merlin.plan_derivation_group (plan_id, derivation_group_name)
      on delete cascade
);

comment on table ui.seen_sources is e''
  'Tracks whether a plan (specifically any of its contributors/owners) has acknowledged that a source is now associated with a plan by virtue of being a member of an associated derivation group.\n'
  'Membership indicates that the new source has been acknowledged and is now understood to be a member.\n'
  'A source in external_source that is part of a derivation group associated with this plan but not in this table is unacknowledged.\n'
  'Acknowledgements are performed in the UI, and upon doing so new entries are appended to this table.';

comment on column ui.seen_sources.plan_id is e''
  'The plan that any new source is now associated with by virtue of being a member of the named derivation group.';
comment on column ui.seen_sources.derivation_group_name is e''
  'The derivation group of the plan is associated with.';
comment on column ui.seen_sources.last_acknowledged_at is e''
  'The time at which changes to the derivation group were last acknowledged.';

-- add a trigger that adds to seen sources whenever an association is made
create function ui.add_seen_source_on_assoc()
  returns trigger
  language plpgsql as $$
begin
  insert into ui.seen_sources values (new.plan_id, new.derivation_group_name);
  return new;
end;
$$;

create trigger add_seen_source_on_assoc
after insert on merlin.plan_derivation_group
  for each row execute function ui.add_seen_source_on_assoc();
