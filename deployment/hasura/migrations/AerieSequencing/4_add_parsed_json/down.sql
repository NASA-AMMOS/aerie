comment on column command_dictionary.parsed_json is null;

alter table command_dictionary
  drop column parsed_json;

call migrations.mark_migration_rolled_back('4');
