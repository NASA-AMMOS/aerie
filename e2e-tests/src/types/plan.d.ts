type CreatePlan = {
  endTime: string;
  id: number;
  modelId: number;
  name: string;
  revision: number;
  startTime: string;
};

type CreatePlanInput = {
  duration: string;
  model_id: number;
  name: string;
  start_time: string;
};

type Plan = {
  activity_directives: ActivityDirective[];
  constraints: Constraint[];
  duration: string;
  endTime: string;
  id: number;
  model: Model;
  name: string;
  revision: number;
  scheduling_specifications: Pick<SchedulingSpec, 'id'>[];
  simulations: Simulation[];
  startTime: string;
};
