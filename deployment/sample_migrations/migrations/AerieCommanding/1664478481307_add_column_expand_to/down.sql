----- NOTE: Test file, should not be used in real database migration

--- Database Change
alter table "public"."activity_instance_commands" drop column "expand_to" cascade;

--- Data migration logic

--- Database Check
