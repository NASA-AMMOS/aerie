create table ui.extension_roles (
  extension_id  integer not null references ui.extensions(id)
    on update cascade
    on delete cascade,
  role text not null,
  primary key (extension_id, role)
);

comment on table ui.extension_roles is e''
  'A mapping of extensions to what roles can access them.';
comment on column ui.extension_roles.extension_id is e''
  'The extension that the role is defined for.';
comment on column ui.extension_roles.role is e''
  'The role that is allowed to access the extension.';
