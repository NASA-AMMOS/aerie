-- Bulk versions of Anchor Deletion
drop function hasura_functions.delete_activity_by_pk_reanchor_plan_start_bulk(_activity_ids int[], _plan_id int);
drop function hasura_functions.delete_activity_by_pk_reanchor_to_anchor_bulk(_activity_ids int[], _plan_id int);
drop function hasura_functions.delete_activity_by_pk_delete_subtree_bulk(_activity_ids int[], _plan_id int);
call migrations.mark_migration_rolled_back('4');

