export interface ActivityInstance {
  activityId: string;
  activityType: string;
  backgroundColor: string;
  constraints: ActivityInstanceConstraint[];
  duration: number;
  end: number;
  endTimestamp: string;
  intent: string;
  listeners?: string[];
  name: string;
  parameters: ActivityInstanceParameter[];
  start: number;
  startTimestamp: string;
  textColor: string;
  y: number | null;
}

export interface ActivityInstanceConstraint {
  name: string;
  type: string;
}

export interface ActivityInstanceParameter {
  defaultValue?: string;
  name: string;
  range?: string[];
  type: string;
  value: string;
}

export interface ActivityType {
  activityClass: string;
  listeners: string[];
  parameters: ActivityTypeParameter[];
  typeName: string;
}

export interface ActivityTypeParameter {
  name: string;
  type: string;
}

export interface Adaptation {
  id: string;
  location: string;
  mission: string;
  name: string;
  owner: string;
  version: string;
}

export interface CommandDictionary {
  id: string;
  name: string;
  version: null | string;
}

export interface MpsCommand {
  name: string;
  parameters: MpsCommandParameter[];
}

export interface MpsCommandParameter {
  defaultValue?: string;
  help?: string;
  name?: string;
  range?: string;
  type?: string;
  units?: string;
}

export interface Plan {
  adaptationId: string;
  endTimestamp: string;
  id: string;
  name: string;
  startTimestamp: string;
}

export interface Schedule {
  id?: string;
}
