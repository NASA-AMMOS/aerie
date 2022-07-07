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

/*
//TODO: adjust this when more than just linear dynamics are possible!
type RealDynamics = {
  initial: number;
  rate: number;
};

type DiscreteDynamics = true | false;

type Segment = {
  duration: number;
  dynamics: RealDynamics | DiscreteDynamics;
};

type ProfileType = "real" | "discrete"


type ProfileSet = {
  name: string;
  type: ProfileType;
  schema?: ValueSchema; //if discrete
  segments: Segment[];
};
*/

type ExternalDataset = {
  plan_id: number;
  datasetStart: string;
  profileSet: string; //I was unsuccessful in expanding this into a proper profile set object with respect to getting
                      // the graphql types right as well. As a result, it is passed as a string and is string formatted!
};
