alter table plan
  add column description text;
comment on column plan.description is e''
  'A human-readable description for this plan and its contents.';

call migrations.mark_migration_applied('27');
