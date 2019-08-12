/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import {
  ActivityInstance,
  Plan,
  StringTMap,
  TimeRange,
} from '../../shared/models';

export const clearSelectedActivity = createAction(
  '[plan] clear_selected_activity',
);

export const clearSelectedPlan = createAction('[plan] clear_selected_plan');

export const createActivity = createAction(
  '[plan] create_activity',
  props<{ data: ActivityInstance; planId: string }>(),
);

export const createActivityFailure = createAction(
  '[plan] create_activity_failure',
  props<{ error: Error }>(),
);

export const createActivitySuccess = createAction(
  '[plan] create_activity_success',
  props<{ activities: ActivityInstance[]; planId: string }>(),
);

export const createPlan = createAction(
  '[plan] create_plan',
  props<{ plan: Plan }>(),
);

export const createPlanFailure = createAction(
  '[plan] create_plan_failure',
  props<{ error: Error }>(),
);

export const createPlanSuccess = createAction(
  '[plan] create_plan_success',
  props<{ plan: Plan }>(),
);

export const deleteActivity = createAction(
  '[plan] delete_activity',
  props<{ activityId: string; planId: string }>(),
);

export const deleteActivityFailure = createAction(
  '[plan] delete_activity_failure',
  props<{ error: Error }>(),
);

export const deleteActivitySuccess = createAction(
  '[plan] delete_activity_success',
  props<{ activityId: string }>(),
);

export const deletePlan = createAction(
  '[plan] delete_plan',
  props<{ planId: string }>(),
);

export const deletePlanFailure = createAction(
  '[plan] delete_plan_failure',
  props<{ error: Error }>(),
);

export const deletePlanSuccess = createAction(
  '[plan] delete_plan_success',
  props<{ deletedPlanId: string }>(),
);

export const fetchActivitiesFailure = createAction(
  '[plan] fetch_activities_failure',
  props<{ error: Error }>(),
);

export const fetchPlansFailure = createAction(
  '[plan] fetch_plans_failure',
  props<{ error: Error }>(),
);

export const selectActivity = createAction(
  '[plan] select_activity',
  props<{ id: string | null }>(),
);

export const setActivities = createAction(
  '[plan] set_activities',
  props<{ activities: ActivityInstance[]; activityId?: string }>(),
);

export const setPlans = createAction(
  '[plan] set_plans',
  props<{ plans: Plan[] }>(),
);

export const setPlansAndSelectedPlan = createAction(
  '[plan] set_plans_and_selected_plan',
  props<{ planId: string; plans: Plan[] }>(),
);

export const updateActivity = createAction(
  '[plan] update_activity',
  props<{ activityId: string; planId: string; update: StringTMap<any> }>(),
);

export const updateActivityFailure = createAction(
  '[plan] update_activity_failure',
  props<{ error: Error }>(),
);

export const updateActivitySuccess = createAction(
  '[plan] update_activity_success',
  props<{ activityId: string; update: StringTMap<any> }>(),
);

export const updatePlan = createAction(
  '[plan] update_plan',
  props<{ planId: string; update: StringTMap<any> }>(),
);

export const updatePlanFailure = createAction(
  '[plan] update_plan_failure',
  props<{ error: Error }>(),
);

export const updatePlanSuccess = createAction('[plan] update_plan_success');

export const updateViewTimeRange = createAction(
  '[plan] update_view_time_range',
  props<{ viewTimeRange: TimeRange }>(),
);
