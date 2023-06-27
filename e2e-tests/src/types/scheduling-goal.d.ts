type SchedulingDslTypesResponse = {
  reason: string;
  status: 'failure' | 'success';
  typescriptFiles: TypescriptFile[];
};

type TypescriptFile = {
  filePath: string;
  content: string;
}

type SchedulingGoal = {
  analyses: SchedulingGoalAnalysis[];
  author: number | null;
  created_date: string;
  definition: string;
  description: string | null;
  id: number;
  last_modified_by: number | null;
  model_id: number;
  modified_date: string;
  name: string;
  revision: number;
};

type SchedulingGoalAnalysis = {
  satisfied: boolean;
  satisfying_activities: { activity_id: number }[];
  satisfying_activities_aggregate: { aggregate: { count: number } };
};

type SchedulingGoalInsertInput = Omit<
    SchedulingGoal,
    'analyses' | 'created_date' | 'id' | 'modified_date' | 'revision'
    >;

type SchedulingResponseStatus = 'pending' | 'complete' | 'failed' | 'incomplete';

type SchedulingResponse = {
  reason: any;
  status: SchedulingResponseStatus;
  analysisId: number;
  datasetId: number | null;
};

type SchedulingSpec = {
  horizon_end: string;
  horizon_start: string;
  id: number;
  plan_id: number;
  plan_revision: number;
  revision: number;
  simulation_arguments: ArgumentsMap;
  analysis_only: boolean;
};

type SchedulingSpecInsertInput = Omit<SchedulingSpec, 'id' | 'revision'>;

type SchedulingSpecGoal = {
  goal: SchedulingGoal;
  priority: number;
  specification_id: number;
};

type SchedulingSpecGoalInsertInput = {
  goal_id: number;
  priority: number;
  specification_id: number;
};
