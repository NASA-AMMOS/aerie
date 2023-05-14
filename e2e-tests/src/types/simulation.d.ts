type Simulation = {
  arguments: ArgumentsMap;
  id: number;
  template: SimulationTemplate | null;
  simulation_start_time: string | null;
  simulation_end_time: string | null;
};

type UpdateSimulationBoundsInput = {
  plan_id: number,
  simulation_start_time: string,
  simulation_end_time: string
}

type SimulationTemplate = {
  arguments: ArgumentsMap;
  description: string;
  id: number;
};

type InsertSimulationTemplateInput = {
  arguments: ArgumentsMap;
  description: string;
  model_id: number;
}

type Resource = {
  name: string;
  schema: ValueSchema;
  startTime: string;
  values: ResourceValue[];
};

type ResourceType = {
  name: string;
  schema: ValueSchema;
};

type ResourceValue = {
  x: number;
  y: number | string;
};

type SimulationResponseActivity = Omit<ActivityDirective, 'id' | 'startTime'> & {
  computedAttributes: string;
  startTimestamp: string;
};

type SimulationResponseStatus = 'pending' | 'complete' | 'failed' | 'incomplete';

type SimulationResponse = {
  reason: any;
  status: SimulationResponseStatus;
  simulationDatasetId: number;
};

type SimulationDataset = {
  canceled: boolean;
  simulation_start_time: string;
  simulation_end_time: string;
  simulated_activities: SimulatedActivity[];
};

type SimulatedActivity = {
  activity_directive: {id: number} | null;
  duration: string | null;
  start_time: string;
  start_offset: string;
}
