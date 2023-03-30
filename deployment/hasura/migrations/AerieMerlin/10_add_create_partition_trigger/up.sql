create function call_create_partition()
  returns trigger
  security invoker
  language plpgsql as $$ begin
    perform allocate_dataset_partitions(new.id);
return new;
end $$;

create trigger create_partition_on_simulation
  after insert on dataset
  for each row
  execute function call_create_partition();

call migrations.mark_migration_applied('10')
