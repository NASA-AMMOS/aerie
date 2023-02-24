-- profile_segment
drop trigger insert_update_profile_segment_trigger on profile_segment;
drop function profile_segment_integrity_function;

alter table profile_segment
  add constraint profile_segment_owned_by_profile
    foreign key (profile_id)
      references profile
      on update cascade
      on delete cascade;

-- event
drop trigger insert_update_event_trigger on event;
drop function event_integrity_function;

alter table event
  add constraint event_owned_by_topic
    foreign key (dataset_id, topic_index)
      references topic
      on update cascade
      on delete cascade;

-- span
drop procedure span_add_foreign_key_to_partition;

do $$
  declare
    dataset_id integer;
  begin
    for dataset_id in select id from dataset
      loop
        if exists(select from pg_tables where schemaname = 'public' and tablename = 'span_' || dataset_id) then
          execute 'alter table span_' || dataset_id || ' drop constraint span_has_parent_span;';
        end if;
      end loop;
  end
$$;

drop trigger insert_update_span_trigger on span;
drop function span_integrity_function;

alter table span
  add constraint span_owned_by_dataset
    foreign key (dataset_id)
      references dataset
      on update cascade
      on delete cascade;

alter table span
  add constraint span_has_parent_span
    foreign key (dataset_id, parent_id)
      references span
      on update cascade
      on delete cascade;

-- profile
drop trigger delete_profile_trigger on profile;
drop function delete_profile_cascade;

drop trigger update_profile_trigger on profile;
drop function update_profile_cascade;

-- topic
drop trigger delete_topic_trigger on topic;
drop function delete_topic_cascade;

drop trigger update_topic_trigger on topic;
drop function update_topic_cascade;

-- dataset
drop function allocate_dataset_partitions;
drop trigger delete_dataset_trigger on dataset;
drop function delete_dataset_cascade;

call migrations.mark_migration_rolled_back('2');
