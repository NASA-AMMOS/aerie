alter table command_dictionary
  add column parsed_json jsonb not null;

comment on column command_dictionary.parsed_json is e''
  'The XML that has been parsed and converted to JSON';

call migrations.mark_migration_applied('4');
