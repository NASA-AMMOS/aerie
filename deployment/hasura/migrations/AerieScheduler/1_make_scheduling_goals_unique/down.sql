/*
This script restores the schema to its state before the migration. It is to be used if the Aerie upgrade is deemed,
unsuccessful, and the database needs to be restored to compatibility with the previous version of Aerie.

Note that this migration does not affect Aerie compatibility - it does not violate any assumptions that Aerie makes.
The only possible compatibility issue is if API clients assume that they can share goals across specifications.

Note that it does not restore data to its state before the migration. If you ran prepare.sql, the copies of your shared
goals are left alone by this script. If you need to restore those goals to the way they were, you can do this manually
via SQL or GraphQL using the output of prepare.sql to see which goals were copied.

Essentially, that would involve:
1. Update the scheduling_specification_goals entries to point to the original goal id
2. (Optional) Delete the new copies of the goals.
*/

alter table scheduling_specification_goals drop constraint if exists scheduling_specification_unique_goal_id;

select mark_migration_rolled_back('1');
