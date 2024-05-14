create table merlin.plan_collaborators(
  plan_id int not null,
  collaborator text not null,

  constraint plan_collaborators_pkey
    primary key (plan_id, collaborator),
  constraint plan_collaborators_plan_id_fkey
    foreign key (plan_id) references merlin.plan
    on update cascade
    on delete cascade,
  constraint plan_collaborator_collaborator_fkey
    foreign key (collaborator) references permissions.users
        on update cascade
        on delete cascade
);

comment on table merlin.plan_collaborators is e''
  'A collection of users who collaborate on the plan alongside the plan''s owner.';
comment on column merlin.plan_collaborators.plan_id is e''
  'The plan the user is a collaborator on.';
comment on column merlin.plan_collaborators.collaborator is e''
  'The username of the collaborator';
