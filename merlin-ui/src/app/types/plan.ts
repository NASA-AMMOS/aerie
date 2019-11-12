import { SActivityInstance } from './activity-instance';
import { StringTMap } from './string-t-map';

export interface Id {
  id: string;
}

export interface CPlan {
  activityInstanceIds: string[];
  adaptationId: string;
  endTimestamp: string;
  id: string;
  name: string;
  startTimestamp: string;
}
export type CPlanMap = StringTMap<CPlan>;

export interface SPlan {
  activityInstances: StringTMap<SActivityInstance>;
  adaptationId: string;
  endTimestamp: string;
  name: string;
  startTimestamp: string;
}
export type SPlanMap = StringTMap<SPlan>;
