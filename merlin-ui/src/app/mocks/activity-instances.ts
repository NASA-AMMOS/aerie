import { CActivityInstanceMap, SActivityInstanceMap } from '../types';

export const activityInstanceId = '5dc99ef353c09f6736c7072d';

export const sActivityInstanceMap: SActivityInstanceMap = {
  [activityInstanceId]: {
    parameters: {
      peelDirection: 'down',
    },
    startTimestamp: '2020-000T00:00:00',
    type: 'PeelBanana',
  },
};
export const sActivityInstance = sActivityInstanceMap[activityInstanceId];

export const cActivityInstanceMap: CActivityInstanceMap = {
  [activityInstanceId]: {
    id: activityInstanceId,
    parameters: {
      peelDirection: {
        name: 'peelDirection',
        value: 'down',
      },
    },
    startTimestamp: '2020-000T00:00:00',
    type: 'PeelBanana',
  },
};
export const cActivityInstance = cActivityInstanceMap[activityInstanceId];
