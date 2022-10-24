create table merge_request_comment(
  comment_id integer generated always as identity,
  merge_request_id integer,
  comment_text text not null,

  constraint merge_request_comment_natural_key
    primary key (comment_id, merge_request_id),
  constraint comment_owned_by_merge_request
    foreign key (merge_request_id)
    references merge_request
    on delete cascade
);

