create table extension_roles (
  extension_id  integer not null references extensions(id)
    on update cascade
    on delete cascade,
  role text not null,
  primary key (extension_id, role)
);

comment on table extension_roles is e''
  'A mapping of extensions to what roles can access them.';
comment on column extension_roles.extension_id is e''
  'The extension that the role is defined for.';
comment on column extension_roles.role is e''
  'The role that is allowed to access the extension.';
