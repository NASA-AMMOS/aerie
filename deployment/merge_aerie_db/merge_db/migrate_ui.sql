begin;
-- Move the contents of "public" to "ui"
alter schema public rename to ui;
comment on schema ui is 'UI Service Schema';
create schema public;
comment on schema public is 'standard public schema';

-- Add PGCrypto back to "public"
create extension pgcrypto with schema public;

-- Update Triggers
drop trigger extensions_set_timestamp on ui.extensions;
drop function ui.extensions_set_updated_at();
create trigger extensions_set_timestamp
  before update on ui.extensions
  for each row
execute function util_functions.set_updated_at();

drop trigger set_timestamp on ui.view;
drop function ui.view_set_updated_at();
create trigger set_timestamp
before update on ui.view
for each row
execute function util_functions.set_updated_at();

-- Add Missing FKeys, Handling Orphans First
delete from ui.extension_roles er
  where not exists(
   select from permissions.user_roles ur
   where er.role = ur.role);
alter table ui.extension_roles
  add foreign key (role)
    references permissions.user_roles (role)
    on update cascade
    on delete cascade;

update ui.extensions
  set owner = null
  where not exists(
   select from permissions.users u
   where owner = u.username);
alter table ui.extensions
  add foreign key (owner)
    references permissions.users (username)
    on update cascade
    on delete set null;

update ui.view
  set owner = null
  where not exists(
   select from permissions.users u
   where owner = u.username);
alter table ui.view
  add foreign key (owner)
    references permissions.users (username)
    on update cascade
    on delete set null;
end;
