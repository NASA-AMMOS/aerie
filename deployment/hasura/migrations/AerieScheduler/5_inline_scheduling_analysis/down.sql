-- Create Scheduling Analysis
create table scheduling_analysis (
  id integer,
  constraint scheduling_analysis_synthetic_key
    primary key (id)
);
comment on table scheduling_analysis is e''
  'The analysis associated with a scheduling run';
comment on column scheduling_analysis.id is e''
  'The synthetic identifier for this scheduling analysis.';

-- Insert all values into scheduling analysis
insert into scheduling_analysis (id)
select analysis_id from scheduling_request;

-- Update FK relationships
alter table scheduling_goal_analysis_created_activities
  drop constraint created_activities_references_scheduling_request,
  add constraint created_activities_references_scheduling_analysis
    foreign key (analysis_id)
      references scheduling_analysis
      on update cascade
      on delete cascade;

alter table scheduling_goal_analysis
  drop constraint scheduling_goal_analysis_references_scheduling_request,
  add constraint scheduling_goal_analysis_references_scheduling_analysis
    foreign key (analysis_id)
      references scheduling_analysis
      on update cascade
      on delete cascade;

alter table scheduling_goal_analysis_satisfying_activities
  drop constraint satisfying_activities_references_scheduling_request,
  add constraint satisfying_activities_references_scheduling_analysis
    foreign key (analysis_id)
      references scheduling_analysis
      on update cascade
      on delete cascade;

alter table scheduling_request
  add constraint scheduling_request_references_analysis
    foreign key(analysis_id)
      references scheduling_analysis
      on update cascade
      on delete cascade,
  alter column analysis_id drop identity;

-- Add Triggers
create function create_scheduling_analysis()
returns trigger
security definer
language plpgsql as $$begin
  insert into scheduling_analysis
  default values
  returning id into new.analysis_id;
return new;
end$$;

do $$ begin
create trigger create_scheduling_analysis_trigger
  before insert on scheduling_request
  for each row
  execute function create_scheduling_analysis();
exception
  when duplicate_object then null;
end $$;

-- Add "Generated Always as Identity"
alter table scheduling_analysis
  alter column id add generated always as identity;

call migrations.mark_migration_rolled_back('5');
