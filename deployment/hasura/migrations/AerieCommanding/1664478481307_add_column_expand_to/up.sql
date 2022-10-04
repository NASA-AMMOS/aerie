----- NOTE: Test file, should not be used in real database migration

--- Database Change
alter table "public"."activity_instance_commands" add column "expand_to" int4;
comment on column "public"."activity_instance_commands"."expand_to" is E'how many commands that were expanded to';
alter table "public"."activity_instance_commands" alter column "expand_to" set default 0;
alter table "public"."activity_instance_commands" alter column "expand_to" drop not null;

--- Data migration logic

--- Database Check
select expand_to from "public"."activity_instance_commands";
