create table merge_request_comment(
  comment_id integer generated always as identity primary key,
  merge_request_id integer,
  commenter_username text not null default '',
  comment_text text not null,

  constraint comment_owned_by_merge_request
    foreign key (merge_request_id)
    references merge_request
    on delete cascade
);
;

