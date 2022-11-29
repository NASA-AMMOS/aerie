/*
This script will make copies of any goals that are shared across multiple specifications.
*/
create schema "migration_1_make_scheduling_goals_unique";

create table "migration_1_make_scheduling_goals_unique".spec_goals_to_leave_alone(
  specification_id integer,
  goal_id integer unique
);

insert into "migration_1_make_scheduling_goals_unique".spec_goals_to_leave_alone (goal_id, specification_id)
select goal_id, min(specification_id) from scheduling_specification_goals group by goal_id;

alter table scheduling_goal add column specification_id integer default null;
alter table scheduling_goal add column old_goal_id integer default null;

insert into scheduling_goal (
  revision,
  name,
  definition,
  model_id,
  description,
  author,
  last_modified_by,
  created_date,
  modified_date,
  specification_id,
  old_goal_id)
(select revision,
       name,
       definition,
       model_id,
       description,
       author,
       last_modified_by,
       created_date,
       modified_date,
       ssg.specification_id,
       id
from scheduling_goal
join scheduling_specification_goals ssg
on id = goal_id
where (goal_id, ssg.specification_id) not in
      (select goal_id, spec_goals_to_leave_alone.specification_id
       from "migration_1_make_scheduling_goals_unique".spec_goals_to_leave_alone)
);

update scheduling_specification_goals ssg
set goal_id = g.id
from scheduling_goal g
where g.specification_id = ssg.specification_id and g.old_goal_id = ssg.goal_id;

alter table scheduling_goal drop column specification_id;
alter table scheduling_goal drop column old_goal_id;
drop table "migration_1_make_scheduling_goals_unique".spec_goals_to_leave_alone;
drop schema "migration_1_make_scheduling_goals_unique";
