-- Create a table to track which sources the user has and has not seen added/removed
create table ui.seen_sources
(
    username text not null,
    external_source_name text not null,
    external_source_type text not null, -- included for ease of filtering, though not part of pkey
    derivation_group text not null,

    constraint seen_sources_pkey
      primary key (username, external_source_name, derivation_group),
    constraint seen_sources_references_user
      foreign key (username)
      references permissions.users (username) match simple
      on delete cascade
);

comment on table ui.seen_sources is e''
  'A table for tracking the external sources either acknowledged by each user.';

comment on column ui.seen_sources.username is e''
  'The username of the user that has seen the given source referenced by this entry.\n'
  'A foreign key referencing the permissions.users table.';
comment on column ui.seen_sources.external_source_name is e''
  'The name of the external_source that the user is being marked as having seen in this entry.';
comment on column ui.seen_sources.external_source_type is e''
  'The external_source_type of the external_source that the user is being marked as having seen in this entry.';
comment on column ui.seen_sources.external_source_type is e''
  'The derivation_group name of the external_source that the user is being marked as having seen in this entry.';
