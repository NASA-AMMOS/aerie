create table merlin.merge_request_comment(
  comment_id integer generated always as identity primary key,
  merge_request_id integer,
  commenter_username text,
  comment_text text not null,

  constraint comment_owned_by_merge_request
    foreign key (merge_request_id)
    references merlin.merge_request
    on delete cascade,
  constraint merge_request_commenter_exists
    foreign key (commenter_username)
    references permissions.users
    on update cascade
    on delete set null
);

comment on table merlin.merge_request_comment is e''
  'A comment left on a given merge request.';
comment on column merlin.merge_request_comment.comment_id is e''
  'The synthetic identifier for this comment.';
comment on column merlin.merge_request_comment.merge_request_id is e''
  'The id of the merge request associated with this comment.';
comment on column merlin.merge_request_comment.commenter_username is e''
  'The user who left this comment.';
comment on column merlin.merge_request_comment.comment_text is e''
  'The contents of this comment.';

