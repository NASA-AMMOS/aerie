create table plan_collaborators(
  plan_id int not null,
  collaborator integer not null,

  constraint plan_collaborators_pkey
    primary key (plan_id, collaborator),
  constraint plan_collaborators_plan_id_fkey
    foreign key (plan_id) references plan
    on update cascade
    on delete cascade,
  constraint plan_collaborator_collaborator_fkey
    foreign key (collaborator) references metadata.users
        on update cascade
        on delete cascade
);

comment on table plan_collaborators is e''
  'A collection of users who collaborate on the plan alongside the plan''s owner.';
comment on column plan_collaborators.plan_id is e''
  'The plan the user is a collaborator on.';
comment on column plan_collaborators.collaborator is e''
  'The user id of the collaborator';
