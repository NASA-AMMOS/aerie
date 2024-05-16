create view sequencing.expansion_set_rule_view as
select str.set_id,
			 rule.id,
			 rule.activity_type,
			 rule.expansion_logic,
			 rule.parcel_id,
			 rule.authoring_mission_model_id,
			 rule.created_at,
			 rule.updated_at,
			 rule.name,
			 rule.owner,
			 rule.updated_by,
			 rule.description
from sequencing.expansion_set_to_rule str
left join sequencing.expansion_rule rule
on str.rule_id = rule.id;

