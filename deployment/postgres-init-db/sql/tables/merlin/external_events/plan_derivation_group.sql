create table merlin.plan_derivation_group (
    plan_id integer not null,
    derivation_group_name text not null,
    last_acknowledged_at timestamp with time zone default now() not null,

    constraint plan_derivation_group_pkey
      primary key (plan_id, derivation_group_name),
    constraint plan_derivation_group_references_plan_id
      foreign key (plan_id)
      references merlin.plan(id),
    constraint plan_derivation_group_references_derivation_group_name
      foreign key (derivation_group_name)
      references merlin.derivation_group(name)
);

comment on table merlin.plan_derivation_group is e''
  'Links externally imported event sources & plans.\n'
  'Additionally, tracks the last time a plan owner/contributor(s) have acknowledged additions to the derivation group.\n';

comment on column merlin.plan_derivation_group.plan_id is e''
  'The plan with which the derivation group is associated.';
comment on column merlin.plan_derivation_group.derivation_group_name is e''
  'The derivation group being associated with the plan.';
comment on column merlin.plan_derivation_group.last_acknowledged_at is e''
  'The time at which changes to the derivation group were last acknowledged.';

-- if an external source is linked to a plan it cannot be deleted
create function merlin.external_source_pdg_association_delete()
  returns trigger
  language plpgsql as $$
begin
  if exists(select * from merlin.plan_derivation_group pdg where pdg.derivation_group_name = old.derivation_group_name) then
    raise foreign_key_violation
    using message='External source ' || old.key || ' is part of a derivation group that is associated to a plan.';
  end if;
  return null;
end;
$$;

create trigger external_source_pdg_association_delete
before delete on merlin.external_source
  for each row execute function merlin.external_source_pdg_association_delete();
