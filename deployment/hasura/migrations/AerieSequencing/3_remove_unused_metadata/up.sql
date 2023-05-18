-- Sequence
drop trigger set_timestamp on sequence;
drop trigger sequence_set_updated_at_external_trigger on sequence_to_simulated_activity;
drop function sequence_set_updated_at();
drop function sequence_set_updated_at_external();

comment on column sequence.requested_by is null;

alter table sequence
drop column updated_at,
drop column requested_by;

-- Command Dictionary
drop trigger trigger_command_dictionary_set_updated_at on command_dictionary;
drop function command_dictionary_set_updated_at();

alter table command_dictionary
drop column updated_at;

call migrations.mark_migration_applied('3');
