alter table plan drop column description;

call migrations.mark_migration_rolled_back('27');
