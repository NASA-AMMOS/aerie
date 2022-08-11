type ActivityDirectiveId = number;

type ActivityType = {
  name: string;
  parameters: ParametersMap;
};

type ActivityTypesMap = Record<string, ActivityType>;

type ActivityDirective = {
  arguments: ArgumentsMap;
  children: number[] | null;
  duration: number | null;
  id: ActivityDirectiveId;
  parent: number | null;
  startTime: string;
  type: string;
};

type ActivitieDirectivesMap = Record<ActivityDirectiveId, ActivityDirective>;

type ActivityInsertInput = {
  arguments: ArgumentsMap;
  plan_id: number;
  start_offset: string;
  type: string;
};

type ActivitySetInput = Partial<ActivityInsertInput>;
