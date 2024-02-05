create table metadata.snapshot_activity_tags(
  directive_id integer not null,
  snapshot_id integer not null,

  tag_id integer not null references metadata.tags
    on update cascade
    on delete cascade,

  constraint tags_on_existing_snapshot_directive
    foreign key (directive_id, snapshot_id)
      references plan_snapshot_activities
      on update cascade
      on delete cascade,
  primary key (directive_id, snapshot_id, tag_id)
);

comment on table metadata.snapshot_activity_tags is e''
  'The tags associated with an activity directive snapshot.';


create function snapshot_tags_in_review_delete()
  returns trigger
  security definer
language plpgsql as $$
  begin
    if exists(select status from merge_request mr
      where
        (mr.snapshot_id_supplying_changes = old.snapshot_id
        or mr.merge_base_snapshot_id = old.snapshot_id)
      and mr.status = 'in-progress') then
      raise exception 'Cannot delete. Snapshot is in use in an active merge review.';
    end if;
    return old;
  end
$$;

create trigger snapshot_tags_in_review_delete_trigger
  before delete on metadata.snapshot_activity_tags
  for each row
  execute function snapshot_tags_in_review_delete();
