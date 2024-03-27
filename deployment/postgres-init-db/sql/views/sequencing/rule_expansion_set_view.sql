create view sequencing.rule_expansion_set_view as
select str.rule_id,
			 set.id,
			 set.name,
			 set.owner,
			 set.description,
			 set.command_dict_id,
			 set.mission_model_id,
			 set.created_at,
			 set.updated_at,
			 set.updated_by
from sequencing.expansion_set_to_rule str left join sequencing.expansion_set set
  on str.set_id = set.id;
