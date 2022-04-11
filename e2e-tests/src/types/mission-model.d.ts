type MissionModelInsertInput = {
  jar_id: number;
  mission: string;
  name: string;
  version: string;
};

type Model = {
  activityTypes: ActivityType[];
  constraints: Constraint[];
  id: number;
  parameters: { parameters: ParametersMap };
};
