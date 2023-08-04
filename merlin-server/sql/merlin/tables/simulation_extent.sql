create table simulation_extent (
  simulation_dataset_id integer not null primary key,
  extent interval not null,
  constraint simulation_dataset_exists
    foreign key (simulation_dataset_id)
    references simulation_dataset
    on update cascade
    on delete cascade
);

comment on table simulation_extent is e''
  'Tracks the progress of a simulation as the latest achieved offset from the simulation start time. \n'
  'This is expected to be an update-heavy table, so it is to be kept compact to maximize the likelihood of HOT updates and minimize bloat \n'
  'The data in this table is not particularly valuable once a simulation has completed, and can be cleared out periodically';

comment on column simulation_extent.simulation_dataset_id is e''
  'The simulation dataset to which this extent pertains';

comment on column simulation_extent.extent is e''
  'The latest achieved offset from the simulation start time';
