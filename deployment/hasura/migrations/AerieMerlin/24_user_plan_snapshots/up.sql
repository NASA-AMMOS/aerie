-- Update plan_snapshot
alter table plan_snapshot
	drop column name,
	drop column duration,
	drop column start_time,
	add column snapshot_name text,
	add column taken_by text,
	add column taken_at timestamptz not null default now(),
	add constraint snapshot_name_unique_per_plan
		unique (plan_id, snapshot_name);


comment on table plan_snapshot is e''
  'A record of the state of a plan at a given time.';
comment on column plan_snapshot.snapshot_id is e''
	'The identifier of the snapshot.';
comment on column plan_snapshot.plan_id is e''
	'The plan that this is a snapshot of.';
comment on column plan_snapshot.revision is e''
	'The revision of the plan at the time the snapshot was taken.';
comment on column plan_snapshot.snapshot_name is e''
	'A human-readable name for the snapshot.';
comment on column plan_snapshot.taken_by is e''
	'The user who took the snapshot.';
comment on column plan_snapshot.taken_at is e''
	'The time that the snapshot was taken.';

call migrations.mark_migration_applied('24');
