create view activity_directive_validations as(
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
