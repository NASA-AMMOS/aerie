----- NOTE: Test file, should not be used in real database migration

--- Database Change

--Verify column exists and rename column
select missions from "public"."command_dictionary";
alter table "public"."command_dictionary" rename column "missions" to "mission";

--- Data migration logic

--- Database Check
select mission from "public"."command_dictionary";
