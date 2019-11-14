import { StringTMap } from './string-t-map';

export interface CActivityInstanceParameter {
  name: string;
  value: any;
}
export type CActivityInstanceParameterMap = StringTMap<
  CActivityInstanceParameter
>;

export interface SActivityInstanceParameter {
  value: any;
}
export type SActivityInstanceParameterMap = StringTMap<
  SActivityInstanceParameter
>;
