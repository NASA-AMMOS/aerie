----- NOTE: Test file, should not be used in real database migration

--- Database Change

--Verify parameter dictionary table exist
select * from "public"."channel_dictionary";
drop table "public"."channel_dictionary";

--- Data migration logic

--- Database Check

