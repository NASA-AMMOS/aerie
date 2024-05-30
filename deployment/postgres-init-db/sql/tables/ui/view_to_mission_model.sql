create table ui.view_to_mission_model (
  created_at timestamptz not null default now(),
  id integer generated always as identity,
  mission_model_id integer not null,
  updated_at timestamptz not null default now(),
  view_id integer not null,

  constraint view_to_mission_model_primary_key
    primary key (id),

  constraint one_view_per_mission_model
    unique (mission_model_id, view_id),

  foreign key (mission_model_id)
    references merlin.mission_model (id)
    on delete cascade,

  foreign key (view_id)
    references ui.view (id)
    on delete cascade
);

comment on table ui.view_to_mission_model is e''
  'Default views set for a given mission model.';
comment on column ui.view_to_mission_model.created_at is e''
  'The time the default view was set.';
comment on column ui.view_to_mission_model.id is e''
  'Integer primary key of the view to mission model mapping.';
comment on column ui.view_to_mission_model.mission_model_id is e''
  'The mission model id that the view is mapped to.';
comment on column ui.view_to_mission_model.updated_at is e''
  'The time the mission model to view mapping was last updated at.';
comment on column ui.view_to_mission_model.view_id is e''
  'The view id that the mission model is mapped to.';

create trigger set_timestamp
  before update on ui.view_to_mission_model
  for each row
execute function util_functions.set_updated_at();
