/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import {
  ActivityInstance,
  Plan,
  StringTMap,
  TimeRange,
} from '../../shared/models';

export enum PlanActionTypes {
  ClearSelectedActivity = '[plan] clear_selected_activity',
  ClearSelectedPlan = '[plan] clear_selected_plan',
  CreateActivity = '[plan] create_activity',
  CreateActivityFailure = '[plan] create_activity_failure',
  CreateActivitySuccess = '[plan] create_activity_success',
  CreatePlan = '[plan] create_plan',
  CreatePlanFailure = '[plan] create_plan_failure',
  CreatePlanSuccess = '[plan] create_plan_success',
  DeleteActivity = '[plan] delete_activity',
  DeleteActivityFailure = '[plan] delete_activity_failure',
  DeleteActivitySuccess = '[plan] delete_activity_success',
  DeletePlan = '[plan] delete_plan',
  DeletePlanFailure = '[plan] delete_plan_failure',
  DeletePlanSuccess = '[plan] delete_plan_success',
  FetchActivities = '[plan] fetch_activities',
  FetchActivitiesFailure = '[plan] fetch_activities_failure',
  FetchActivitiesSuccess = '[plan] fetch_activities_success',
  FetchPlans = '[plan] fetch_plans',
  FetchPlansFailure = '[plan] fetch_plans_failure',
  FetchPlansSuccess = '[plan] fetch_plans_success',
  SelectActivity = '[plan] select_activity',
  SelectPlan = '[plan] select_plan',
  UpdateActivity = '[plan] update_activity',
  UpdateActivityFailure = '[plan] update_activity_failure',
  UpdateActivitySuccess = '[plan] update_activity_success',
  UpdatePlan = '[plan] update_plan',
  UpdatePlanFailure = '[plan] update_plan_failure',
  UpdatePlanSuccess = '[plan] update_plan_success',
  UpdateViewTimeRange = '[plan] update_view_time_range',
}

export class ClearSelectedActivity implements Action {
  readonly type = PlanActionTypes.ClearSelectedActivity;
}

export class ClearSelectedPlan implements Action {
  readonly type = PlanActionTypes.ClearSelectedPlan;
}

export class CreateActivity implements Action {
  readonly type = PlanActionTypes.CreateActivity;
  constructor(public planId: string, public data: ActivityInstance) {}
}

export class CreateActivityFailure implements Action {
  readonly type = PlanActionTypes.CreateActivityFailure;
  constructor(public error: Error) {}
}

export class CreateActivitySuccess implements Action {
  readonly type = PlanActionTypes.CreateActivitySuccess;
  constructor(public planId: string, public activity: ActivityInstance) {}
}

export class CreatePlan implements Action {
  readonly type = PlanActionTypes.CreatePlan;
  constructor(public plan: Plan) {}
}

export class CreatePlanFailure implements Action {
  readonly type = PlanActionTypes.CreatePlanFailure;
  constructor(public error: Error) {}
}

export class CreatePlanSuccess implements Action {
  readonly type = PlanActionTypes.CreatePlanSuccess;
  constructor(public plan: Plan) {}
}

export class DeleteActivity implements Action {
  readonly type = PlanActionTypes.DeleteActivity;
  constructor(public planId: string, public activityId: string) {}
}

export class DeleteActivityFailure implements Action {
  readonly type = PlanActionTypes.DeleteActivityFailure;
  constructor(public error: Error) {}
}

export class DeleteActivitySuccess implements Action {
  readonly type = PlanActionTypes.DeleteActivitySuccess;
  constructor(public activityId: string) {}
}

export class DeletePlan implements Action {
  readonly type = PlanActionTypes.DeletePlan;
  constructor(public planId: string) {}
}

export class DeletePlanFailure implements Action {
  readonly type = PlanActionTypes.DeletePlanFailure;
  constructor(public error: Error) {}
}

export class DeletePlanSuccess implements Action {
  readonly type = PlanActionTypes.DeletePlanSuccess;
  constructor(public deletedPlanId: string) {}
}

export class FetchActivities implements Action {
  readonly type = PlanActionTypes.FetchActivities;
  constructor(public planId: string, public activityId: string | null) {}
}

export class FetchActivitiesFailure implements Action {
  readonly type = PlanActionTypes.FetchActivitiesFailure;
  constructor(public error: Error) {}
}

export class FetchActivitiesSuccess implements Action {
  readonly type = PlanActionTypes.FetchActivitiesSuccess;
  constructor(
    public planId: string,
    public activityId: string | null,
    public activities: ActivityInstance[],
  ) {}
}

export class FetchPlans implements Action {
  readonly type = PlanActionTypes.FetchPlans;
}

export class FetchPlansFailure implements Action {
  readonly type = PlanActionTypes.FetchPlansFailure;
  constructor(public error: Error) {}
}

export class FetchPlansSuccess implements Action {
  readonly type = PlanActionTypes.FetchPlansSuccess;
  constructor(public data: Plan[]) {}
}

export class SelectActivity implements Action {
  readonly type = PlanActionTypes.SelectActivity;
  constructor(public id: string | null) {}
}

export class SelectPlan implements Action {
  readonly type = PlanActionTypes.SelectPlan;
  constructor(public id: string) {}
}

export class UpdateActivity implements Action {
  readonly type = PlanActionTypes.UpdateActivity;
  constructor(
    public planId: string,
    public activityId: string,
    public update: StringTMap<any>,
  ) {}
}

export class UpdateActivityFailure implements Action {
  readonly type = PlanActionTypes.UpdateActivityFailure;
  constructor(public error: Error) {}
}

export class UpdateActivitySuccess implements Action {
  readonly type = PlanActionTypes.UpdateActivitySuccess;
  constructor(public activityId: string, public update: StringTMap<any>) {}
}

export class UpdatePlan implements Action {
  readonly type = PlanActionTypes.UpdatePlan;
  constructor(public planId: string, public update: StringTMap<any>) {}
}

export class UpdatePlanFailure implements Action {
  readonly type = PlanActionTypes.UpdatePlanFailure;
  constructor(public error: Error) {}
}

export class UpdatePlanSuccess implements Action {
  readonly type = PlanActionTypes.UpdatePlanSuccess;
}

export class UpdateViewTimeRange implements Action {
  readonly type = PlanActionTypes.UpdateViewTimeRange;
  constructor(public viewTimeRange: TimeRange) {}
}

export type PlanActions =
  | ClearSelectedActivity
  | ClearSelectedPlan
  | CreatePlan
  | CreatePlanFailure
  | CreatePlanSuccess
  | DeleteActivity
  | DeleteActivityFailure
  | DeleteActivitySuccess
  | DeletePlan
  | DeletePlanFailure
  | DeletePlanSuccess
  | FetchActivities
  | FetchActivitiesFailure
  | FetchActivitiesSuccess
  | FetchPlans
  | FetchPlansFailure
  | FetchPlansSuccess
  | CreateActivity
  | CreateActivityFailure
  | CreateActivitySuccess
  | SelectActivity
  | SelectPlan
  | UpdateActivity
  | UpdateActivityFailure
  | UpdateActivitySuccess
  | UpdatePlan
  | UpdatePlanFailure
  | UpdatePlanSuccess
  | UpdateViewTimeRange;
