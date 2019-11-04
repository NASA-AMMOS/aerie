import { StringTMap } from './string-t-map';

export interface CActivityTypeParameter {
  name: string;
  type: string;
}
export type CActivityTypeParameterMap = StringTMap<CActivityTypeParameter>;

export type SActivityTypeParameter = Omit<CActivityTypeParameter, 'name'>;
export type SActivityTypeParameterMap = StringTMap<SActivityTypeParameter>;
