create type event_t as enum ('begin', 'end', 'wait');

create table lifecycle_event (
    id int generated always as identity,
    timeline_segment_id int,
    activity_id int,
    event event_t,

    constraint lifecycle_event_synthetic_key
        primary key (id),

    constraint timeline_segment_includes_lifecycle_events
        foreign key (timeline_segment_id)
        references timeline_segment (id)
        on update cascade
        on delete cascade,

    constraint lifecycle_event_is_about_activity
        foreign key (activity_id)
        references activity (id)
        on update cascade
        on delete cascade
);

comment on table lifecycle_event is e''
    'a lifecycle event for a specific activity meant to be streamed out';

comment on column lifecycle_event.timeline_segment_id is e''
    'the timeline_segment that this event belongs to';

comment on column lifecycle_event.activity_id is e''
    'the activity that this lifecycle event relates to';

comment on column lifecycle_event.event is e''
    'an enum representation of the event, one of "begin", "end", "wait"';
