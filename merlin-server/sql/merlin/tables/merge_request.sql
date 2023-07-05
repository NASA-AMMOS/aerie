create table merge_request(
      id integer generated always as identity
        primary key,
      plan_id_receiving_changes integer,
      snapshot_id_supplying_changes integer,
      merge_base_snapshot_id integer not null,
      status merge_request_status default 'pending',
      requester_username text,
      reviewer_username text,
      constraint merge_request_requester_exists
        foreign key (requester_username)
        references metadata.users
        on update cascade
        on delete set null,
      constraint merge_request_reviewer_exists
        foreign key (reviewer_username)
        references metadata.users
        on update cascade
        on delete set null
);

comment on table merge_request is e''
  'A request to merge the state of the activities from one plan onto another.';

comment on column merge_request.id is e''
  'The synthetic identifier for this merge request.';
comment on column merge_request.plan_id_receiving_changes is e''
  'The plan id of the plan to receive changes as a result of this merge request being processed and committed.'
  '\nAlso known as "Target".';
comment on column merge_request.snapshot_id_supplying_changes is e''
  'The snapshot id used to supply changes when this merge request is processed.'
  '\nAlso known as "Source".';
comment on column merge_request.merge_base_snapshot_id is e''
  'The snapshot id that is the nearest common ancestor between the '
  'plan_id_receiving_changes and the snapshot_id_supplying_changes of this merge request.';
comment on column merge_request.status is e''
  'The current status of this merge request.';
comment on column merge_request.requester_username is e''
  'The user who created this merge request.';
comment on column merge_request.reviewer_username is e''
  'The user who reviews this merge request. Is empty until the request enters review.';
