type User = {
  username: string;
  default_role: string;
  allowed_roles: string[];
  session: Record<string, string>;
};

type UserInsert = Omit<User, "allowed_roles" | "session">

type UserAllowedRole = {
  username: string,
  allowed_role: string;
}

type Permission = "NO_CHECK" | "OWNER" | "MISSION_MODEL_OWNER" | "PLAN_OWNER" | "PLAN_COLLABORATOR" | "PLAN_OWNER_COLLABORATOR"

type ActionPermissionSet = {
  simulate: Permission | null,
  schedule: Permission | null,
  insert_ext_dataset: Permission | null,
  check_constraints: Permission | null,
  create_expansion_set: Permission | null,
  create_expansion_rule: Permission | null,
  expand_all_activities: Permission | null,
  resource_samples: Permission | null,
  sequence_seq_json_bulk: Permission | null
}
