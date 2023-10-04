create table extension_roles (
  extension_id  integer not null,
  id integer generated always as identity,
  role text not null,

  constraint extension_roles_primary_key primary key (id),
  constraint extension_roles_to_extension
    foreign key (extension_id)
      references "extensions"
      on delete cascade
);

comment on table extension_roles is e''
  'A mapping of extensions to what roles can access them.';
comment on column extension_roles.extension_id is e''
  'The extension that the role is defined for.';
comment on column extension_roles.role is e''
  'The role that is allowed to access the extension.';
