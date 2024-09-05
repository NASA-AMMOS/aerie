create table merlin.simulation_fincons (
  dataset_id integer null,

  fincons jsonb not null,

  constraint simulation_fincons_references_dataset
    foreign key (dataset_id)
    references merlin.dataset
    on update cascade
    on delete cascade
);
