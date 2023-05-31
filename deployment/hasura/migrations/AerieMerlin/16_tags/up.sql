create schema metadata;
-- Tags
create table metadata.tags(
  id integer generated always as identity
    primary key,
  name text not null unique,
  color text null,
  owner text not null default '',
  created_at timestamptz not null default now(),

  constraint color_is_hex_format
    check (color is null or color ~* '^#[a-f0-9]{6}$' )
);

comment on table metadata.tags is e''
  'All tags usable within an Aerie deployment.';
comment on column metadata.tags.id is e''
  'The index of the tag.';
comment on column metadata.tags.name is e''
  'The name of the tag. Unique within a deployment.';
comment on column metadata.tags.color is e''
  'The color the tag should display as when using a GUI.';
comment on column metadata.tags.owner is e''
  'The user responsible for this tag. '
  '''Mission Model'' is used to represent tags originating from an uploaded mission model'
  '''Aerie Legacy'' is used to represent tags originating from a version of Aerie prior to this table''s creation.';
comment on column metadata.tags.created_at is e''
  'The date this tag was created.';

-- Plan Tags
create table metadata.plan_tags(
  plan_id integer not null references public.plan
      on update cascade
      on delete cascade,
  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,
  primary key (plan_id, tag_id)
);
comment on table metadata.plan_tags is e''
  'The tags associated with a plan. Note: these tags will not be compared during a plan merge.';

-- Activity Type
alter table public.activity_type
add column subsystem2 integer;

insert into metadata.tags(name, color, owner)
select at.subsystem, null, 'Aerie Legacy'
from public.activity_type at
where at.subsystem is not null
on conflict (name) do nothing;

update public.activity_type at
set subsystem2 = t.id
from metadata.tags t
where t.name = at.subsystem;

alter table public.activity_type
drop column subsystem;

alter table public.activity_type
rename column subsystem2 to subsystem;

alter table public.activity_type
add foreign key (subsystem) references metadata.tags
    on update cascade
    on delete restrict;

-- Constraint Tags
create table metadata.constraint_tags (
  constraint_id integer not null references public."constraint"
    on update cascade
    on delete cascade,
  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,
  primary key (constraint_id, tag_id)
);
comment on table metadata.constraint_tags is e''
  'The tags associated with a constraint.';

-- Move existing tags
insert into metadata.tags(name, color, owner)
select unnest(c.tags), null, 'Aerie Legacy' from public."constraint" c
on conflict (name) do nothing;

insert into metadata.constraint_tags(constraint_id, tag_id)
select c.id, t.id
  from metadata.tags t
   inner join public."constraint" c
  on t.name = any(c.tags)
on conflict (constraint_id, tag_id) do nothing;

comment on column public."constraint".tags is null;
alter table public."constraint"
drop column tags;

-- "Unlock" Activity Directive and Snapshot Directive
alter table hasura_functions.begin_merge_return_value
  drop column non_conflicting_activities,
  drop column conflicting_activities;
alter table hasura_functions.get_non_conflicting_activities_return_value
   drop column source,
   drop column target;
alter table hasura_functions.get_conflicting_activities_return_value
   drop column source,
   drop column target,
   drop column merge_base;
alter table hasura_functions.delete_anchor_return_value
  drop column affected_row;

-- Snapshot Activity Tags
create table metadata.snapshot_activity_tags(
  directive_id integer not null,
  snapshot_id integer not null,

  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,

  constraint tags_on_existing_snapshot_directive
    foreign key (directive_id, snapshot_id)
      references plan_snapshot_activities
      on update cascade
      on delete cascade,
  primary key (directive_id, snapshot_id, tag_id)
);
comment on table metadata.snapshot_activity_tags is e''
  'The tags associated with an activity directive snapshot.';

-- Move Existing Tags
insert into metadata.tags(name, color, owner)
select unnest(psa.tags), null, 'Aerie Legacy' from public.plan_snapshot_activities psa
on conflict (name) do nothing;

insert into metadata.snapshot_activity_tags(snapshot_id, directive_id, tag_id)
select psa.snapshot_id, psa.id, t.id
from public.plan_snapshot_activities psa, metadata.tags t
where t.name = any(psa.tags)
on conflict (snapshot_id, directive_id, tag_id) do nothing;

-- Activity Directive Extended View
create function get_tags(_activity_id int, _plan_id int)
  returns jsonb
  security definer
  language plpgsql as $$
  declare
    tags jsonb;
begin
    select  jsonb_agg(json_build_object(
      'id', id,
      'name', name,
      'color', color,
      'owner', owner,
      'created_at', created_at
      ))
    from metadata.tags tags, metadata.activity_directive_tags adt
    where tags.id = adt.tag_id
      and (adt.directive_id, adt.plan_id) = (_activity_id, _plan_id)
    into tags;
    return tags;
end
$$;

drop view activity_directive_extended;
create view activity_directive_extended as
(
  select
    -- Activity Directive Properties
    ad.id as id,
    ad.plan_id as plan_id,
    -- Additional Properties
    ad.name as name,
    get_tags(ad.id, ad.plan_id) as tags,
    ad.source_scheduling_goal_id as source_scheduling_goal_id,
    ad.created_at as created_at,
    ad.last_modified_at as last_modified_at,
    ad.start_offset as start_offset,
    ad.type as type,
    ad.arguments as arguments,
    ad.last_modified_arguments_at as last_modified_arguments_at,
    ad.metadata as metadata,
    ad.anchor_id as anchor_id,
    ad.anchored_to_start as anchored_to_start,
    -- Derived Properties
    get_approximate_start_time(ad.id, ad.plan_id) as approximate_start_time,
    ptd.preset_id as preset_id,
    ap.arguments as preset_arguments
   from activity_directive ad
   left join preset_to_directive ptd on ad.id = ptd.activity_id and ad.plan_id = ptd.plan_id
   left join activity_presets ap on ptd.preset_id = ap.id
);

alter table plan_snapshot_activities
drop column tags;

-- Activity Directives
create table metadata.activity_directive_tags(
  directive_id integer not null,
  plan_id integer not null,

  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,

  constraint tags_on_existing_activity_directive
    foreign key (directive_id, plan_id)
      references activity_directive
      on update cascade
      on delete cascade,
  primary key (directive_id, plan_id, tag_id)
);
comment on table metadata.activity_directive_tags is e''
  'The tags associated with an activity directive.';

  -- Move Existing Tags
insert into metadata.tags(name, color, owner)
select unnest(ad.tags), null, 'Aerie Legacy' from public.activity_directive ad
on conflict (name) do nothing;

insert into metadata.activity_directive_tags(plan_id, directive_id, tag_id)
select ad.plan_id, ad.id, t.id
from public.activity_directive ad
inner join metadata.tags t
on t.name = any(ad.tags)
on conflict (plan_id, directive_id, tag_id) do nothing;

comment on column public.activity_directive.tags is null;
alter table public.activity_directive
drop column tags;

-- "Lock" Activity Directive and Snapshot Directive
alter table hasura_functions.delete_anchor_return_value
  add column affected_row activity_directive;
alter table hasura_functions.get_conflicting_activities_return_value
   add column source plan_snapshot_activities,
   add column target activity_directive,
   add column merge_base plan_snapshot_activities;
alter table hasura_functions.get_non_conflicting_activities_return_value
   add column source plan_snapshot_activities,
   add column target activity_directive;
alter table hasura_functions.begin_merge_return_value
  add column non_conflicting_activities hasura_functions.get_non_conflicting_activities_return_value[],
  add column conflicting_activities hasura_functions.get_conflicting_activities_return_value[];

call migrations.mark_migration_applied('16');
