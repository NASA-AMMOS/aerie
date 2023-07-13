create type metadata.permission
  as enum ('NO_CHECK', 'OWNER', 'MISSION_MODEL_OWNER', 'PLAN_OWNER', 'PLAN_COLLABORATOR', 'PLAN_OWNER_COLLABORATOR',
    'PLAN_OWNER_SOURCE', 'PLAN_COLLABORATOR_SOURCE', 'PLAN_OWNER_COLLABORATOR_SOURCE',
    'PLAN_OWNER_TARGET', 'PLAN_COLLABORATOR_TARGET', 'PLAN_OWNER_COLLABORATOR_TARGET');

create type metadata.action_permission_key
  as enum ('simulate', 'schedule', 'insert_command_dict', 'insert_ext_dataset', 'extend_ext_dataset',
    'check_constraints', 'create_expansion_set', 'create_expansion_rule', 'expand_all_activities',
    'resource_samples', 'sequence_seq_json', 'sequence_seq_json_bulk', 'user_sequence_seq_json',
    'user_sequence_seq_json_bulk', 'get_command_dict_ts');

create type metadata.function_permission_key
  as enum ('apply_preset', 'branch_plan', 'create_merge_rq', 'withdraw_merge_rq', 'begin_merge', 'cancel_merge',
    'commit_merge', 'deny_merge', 'get_conflicting_activities', 'get_non_conflicting_activities', 'set_resolution',
    'set_resolution_bulk', 'delete_activity_subtree', 'delete_activity_subtree_bulk', 'delete_activity_reanchor_plan',
    'delete_activity_reanchor_plan_bulk', 'delete_activity_reanchor', 'delete_activity_reanchor_bulk', 'get_plan_history');
