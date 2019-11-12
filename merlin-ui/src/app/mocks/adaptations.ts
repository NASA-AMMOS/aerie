import { CAdaptationMap, SAdaptationMap } from '../types';

export const adaptationId = '5dc9976c087c9b71346e76a1';

export const sAdaptationMap: SAdaptationMap = {
  [adaptationId]: {
    mission: 'clipper',
    name: 'banananation',
    owner: 'ccamargo',
    version: '1.0.0',
  },
};
export const sAdaptation = sAdaptationMap[adaptationId];

export const cAdaptationMap: CAdaptationMap = {
  [adaptationId]: {
    id: adaptationId,
    mission: 'clipper',
    name: 'banananation',
    owner: 'ccamargo',
    version: '1.0.0',
  },
};
export const cAdaptation = cAdaptationMap[adaptationId];
