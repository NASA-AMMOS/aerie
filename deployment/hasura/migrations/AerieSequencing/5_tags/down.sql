comment on table metadata.expansion_rule_tags is null;

drop table metadata.expansion_rule_tags;
drop schema metadata;

call migrations.mark_migration_rolled_back('5');
