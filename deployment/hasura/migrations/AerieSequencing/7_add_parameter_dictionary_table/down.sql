----- NOTE: Test file, should not be used in real database migration

--- Database Change

--Verify parameter dictionary table exist
select * from "public"."parameter_dictionary";
drop table "public"."parameter_dictionary";

--- Data migration logic

--- Database Check

