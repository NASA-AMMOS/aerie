create table sequencing.sequence_to_simulated_activity (
    simulated_activity_id int not null,
    simulation_dataset_id int not null,
    seq_id text not null,

    constraint sequence_to_simulated_activity_primary_key
        primary key (simulated_activity_id, simulation_dataset_id),

    constraint sequence_to_simulated_activity_activity_instance_id_fkey
        foreign key (seq_id, simulation_dataset_id)
        references sequencing.sequence (seq_id, simulation_dataset_id)
        on delete cascade
);

comment on table sequencing.sequence_to_simulated_activity is e''
  'Join table for sequences and simulated activities.';
comment on column sequencing.sequence_to_simulated_activity.simulated_activity_id is e''
  'ID of the joining simulated activity.';
comment on column sequencing.sequence_to_simulated_activity.simulation_dataset_id is e''
  'ID of the simulation dataset.';
comment on column sequencing.sequence_to_simulated_activity.seq_id is e''
  'ID of the joining sequence.';
comment on constraint sequence_to_simulated_activity_primary_key on sequencing.sequence_to_simulated_activity is e''
  'Primary key constrains one simulated activity id per simulation dataset.';
comment on constraint sequence_to_simulated_activity_activity_instance_id_fkey on sequencing.sequence_to_simulated_activity is e''
  'Foreign key constrains that this join table relates to a sequence id that exists for the simulation dataset.';
