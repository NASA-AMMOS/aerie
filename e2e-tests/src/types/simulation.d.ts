type Simulation = {
  arguments: ArgumentsMap;
  id: number;
  template: SimulationTemplate | null;
};

type SimulationCreation = {
  arguments: ArgumentsMap;
  plan_id: number;
};

type SimulationTemplate = {
  arguments: ArgumentsMap;
  description: string;
  id: number;
};

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

type SimulationResponseActivity = Omit<Activity, 'id' | 'startTime'> & {
  computedAttributes: string;
  startTimestamp: string;
};
