create table metadata.tags(
  id integer generated always as identity
    primary key,
  name text not null unique,
  color text null,
  owner integer,
  created_at timestamptz not null default now(),

  constraint color_is_hex_format
    check (color is null or color ~* '^#[a-f0-9]{6}$' ),
  constraint tags_owner_exists
    foreign key (owner) references metadata.users
    on update cascade
    on delete set null
);

comment on table metadata.tags is e''
  'All tags usable within an Aerie deployment.';
comment on column metadata.tags.id is e''
  'The index of the tag.';
comment on column metadata.tags.name is e''
  'The name of the tag. Unique within a deployment.';
comment on column metadata.tags.color is e''
  'The color the tag should display as when using a GUI.';
comment on column metadata.tags.owner is e''
  'The user responsible for this tag. '
  '''Mission Model'' (ID -1) is used to represent tags originating from an uploaded mission model'
  '''Aerie Legacy'' (ID -2) is used to represent tags originating from a version of Aerie prior to this table''s creation.';
comment on column metadata.tags.created_at is e''
  'The date this tag was created.';
