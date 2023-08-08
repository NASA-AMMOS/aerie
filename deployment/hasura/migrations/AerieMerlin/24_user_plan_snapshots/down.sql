-- Update plan_snapshot
comment on table plan_snapshot is e''
  'A record of the metadata associated with a plan, excluding tags.';
comment on column plan_snapshot.snapshot_id is null;
comment on column plan_snapshot.plan_id is null;
comment on column plan_snapshot.revision is null;
comment on column plan_snapshot.snapshot_name is null;
comment on column plan_snapshot.taken_by is null;
comment on column plan_snapshot.taken_at is null;

alter table plan_snapshot
	drop constraint snapshot_name_unique_per_plan,
	drop column taken_at,
	drop column taken_by,
	drop column snapshot_name,
	add column name text,
	add column duration interval,
	add column start_time timestamptz;

update plan_snapshot ps
set start_time = p.start_time,
    name = p.name,
    duration = p.duration
from plan p
where ps.plan_id = p.id;

alter table plan_snapshot
	alter column name set not null,
	alter column duration set not null ,
	alter column start_time set not null;

call migrations.mark_migration_rolled_back('24');
