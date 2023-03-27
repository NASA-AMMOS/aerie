drop function hasura_functions.get_resources_at_start_offset(_dataset_id int, _start_offset interval);
drop table hasura_functions.resource_at_start_offset_return_value;
call migrations.mark_migration_rolled_back('9');
