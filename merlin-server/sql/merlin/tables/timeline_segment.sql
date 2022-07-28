create table timeline_segment (
    id int generated always as identity,
    simulation_dataset_id int,
    simulation_time text,

    constraint segment_synthetic_key
        primary key (id),

    constraint segment_belongs_to_sim_dataset
        foreign key (simulation_dataset_id)
        references simulation_dataset (id)
        on update cascade
        on delete cascade
);

comment on table timeline_segment is e''
    'a segment of the simulation timeline meant to be streamed out';

comment on column timeline_segment.id is e''
    'the synthetic identifier for this segment';

comment on column timeline_segment.simulation_dataset_id is e''
    'the simulation dataset that this segment belongs to';

comment on column timeline_segment.simulation_time is e''
    'the simulation time the timeline should be stepped up to';
