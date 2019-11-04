import {
  CActivityTypeParameter,
  SActivityTypeParameter,
} from './activity-type-parameter';
import { StringTMap } from './string-t-map';

export interface CActivityType {
  name: string;
  parameters: CActivityTypeParameter[];
}
export type CActivityTypeMap = StringTMap<CActivityType>;

export interface SActivityType {
  parameters: StringTMap<SActivityTypeParameter>;
}
export type SActivityTypeMap = StringTMap<SActivityType>;
