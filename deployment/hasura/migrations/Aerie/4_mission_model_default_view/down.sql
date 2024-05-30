drop trigger set_timestamp on view_to_mission_model;

drop table view_to_mission_model;

call migrations.mark_migration_rolled_back('4');
