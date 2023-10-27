alter table activity_directive_changelog
  add column validation_results jsonb default '{}'::jsonb;

create or replace function notify_validation_workers()
  returns trigger
security definer
  language plpgsql as $$
begin
    perform (
      with payload(activity_directive_id,
                   plan_id,
                   revision,
                   plan_id,
                   model_id,
                   type,
                   arguments) as
             (
               select new.activity_directive_id,
                      NEW.plan_id,
                      NEW.revision,
                      NEW.plan_id,
                      (select model_id from plan where id = NEW.plan_id),
                      NEW.type,
                      NEW.arguments
             )
      select pg_notify('validation_notification', json_strip_nulls(row_to_json(payload))::text)
      from payload
    );
    return null;
end$$;

create trigger notify_validation_workers_trigger
  after insert on activity_directive_changelog
  for each row
  execute function notify_validation_workers();

drop table activity_directive_validations;

create or replace view activity_directive_validations as(
select adc.activity_directive_id as directive_id,
       adc.plan_id,
       adc.revision,
       adc.validation_results
from activity_directive_changelog adc
where adc.revision =
      (select max(revision)
       from activity_directive_changelog adc2
       where (adc.activity_directive_id, adc.plan_id) =
             (adc2.activity_directive_id, adc2.plan_id))
  );
