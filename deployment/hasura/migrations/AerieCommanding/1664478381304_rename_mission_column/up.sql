----- NOTE: Test file, should not be used in real database migration

--- Database Change

--Verify column exists and rename column
select mission from "public"."command_dictionary";
alter table "public"."command_dictionary" rename column "mission" to "missions";

--- Data migration logic

--- Database Check
select missions from "public"."command_dictionary";
