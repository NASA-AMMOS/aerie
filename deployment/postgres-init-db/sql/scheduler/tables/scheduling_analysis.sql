create table scheduling_analysis (
  id integer generated always as identity,

  constraint scheduling_analysis_synthetic_key
    primary key (id)
);

comment on table scheduling_analysis is e''
  'The analysis associated with a scheduling run';
comment on column scheduling_analysis.id is e''
  'The synthetic identifier for this scheduling analysis.';
