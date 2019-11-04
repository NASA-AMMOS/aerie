import { StringTMap } from './string-t-map';

export interface CActivityInstanceParameter {
  name: string;
  type: string;
}
export type CActivityInstanceParameterMap = StringTMap<
  CActivityInstanceParameter
>;

export interface SActivityInstanceParameter {
  type: string;
}
export type SActivityInstanceParameterMap = StringTMap<
  SActivityInstanceParameter
>;
