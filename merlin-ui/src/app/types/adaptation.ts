import { StringTMap } from './string-t-map';

export interface CAdaptation {
  id: string;
  mission: string;
  name: string;
  owner: string;
  version: string;
}
export type CAdaptationMap = StringTMap<CAdaptation>;

export type SAdaptation = Omit<CAdaptation, 'id'>;
export type SAdaptationMap = StringTMap<SAdaptation>;
export type SCreateAdaption = SAdaptation & { file: File };
