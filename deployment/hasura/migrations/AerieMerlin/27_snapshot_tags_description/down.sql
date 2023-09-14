drop table metadata.plan_snapshot_tags;
alter table plan_snapshot drop column description;
alter table plan drop column description;

call migrations.mark_migration_rolled_back('27');
