create view metadata.users_and_roles as
(
  select
    u.id as id,
    u.username as username,
    -- Roles
    u.default_role as hasura_default_role,
    array_agg(r.allowed_role) filter (where r.allowed_role is not null) as hasura_allowed_roles
  from metadata.users u
  left join metadata.users_allowed_roles r
  on r.user_id = u.id
  group by u.id
);

comment on view metadata.users_and_roles is e''
'View a user''s information with their role information';
