import { CPlanMap, SPlanMap } from '../types';
import { activityInstanceId, sActivityInstanceMap } from './activity-instances';
import { adaptationId } from './adaptations';

export const planId = '5dc6062653c09f6736c70725';

export const sPlanMap: SPlanMap = {
  [planId]: {
    activityInstances: sActivityInstanceMap,
    adaptationId,
    endTimestamp: '2020-000T00:00:10',
    name: 'Eat Banana',
    startTimestamp: '2020-000T00:00:00',
  },
};
export const sPlan = sPlanMap[planId];

export const cPlanMap: CPlanMap = {
  [planId]: {
    activityInstanceIds: [activityInstanceId],
    adaptationId,
    endTimestamp: '2020-000T00:00:10',
    id: planId,
    name: 'Eat Banana',
    startTimestamp: '2020-000T00:00:00',
  },
};
export const cPlan = cPlanMap[planId];
