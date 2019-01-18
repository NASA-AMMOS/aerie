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
  RavenActivity,
  RavenActivityDetail,
  RavenActivityUpdate,
  RavenPlan,
  RavenPlanDetail,
  RavenTimeRange,
} from '../../shared/models';

export enum PlanActionTypes {
  FetchPlanDetail = '[plan] fetch_plan_detail',
  FetchPlanDetailSuccess = '[plan] fetch_plan_detail_success',
  FetchPlanDetailFailure = '[plan] fetch_plan_detail_failure',
  FetchPlanList = '[plan] fetch_plan_list',
  FetchPlanListFailure = '[plan] fetch_plan_list_failure',
  FetchPlanListSuccess = '[plan] fetch_plan_list_success',
  OpenPlanFormDialog = '[plan] open_plan_form_dialog',
  RemovePlan = '[plan] remove_plan',
  RemovePlanFailure = '[plan] remove_plan_failure',
  RemovePlanSuccess = '[plan] remove_plan_success',
  SaveActivity = '[plan] save_activity',
  SaveActivityFailure = '[plan] save_activity_failure',
  SaveActivitySuccess = '[plan] save_activity_success',
  SaveActivityDetail = '[plan] save_activity_detail',
  SaveActivityDetailFailure = '[plan] save_activity_detail_failure',
  SaveActivityDetailSuccess = '[plan] save_activity_detail_success',
  SavePlan = '[plan] save_plan',
  SavePlanFailure = '[plan] save_plan_failure',
  SavePlanSuccess = '[plan] save_plan_success',
  SelectActivity = '[plan] select_activity',
  UpdateSelectedActivity = '[plan] update_selected_activity',
  UpdateViewTimeRange = '[plan] update_view_time_range',
}

export class FetchPlanDetail implements Action {
  readonly type = PlanActionTypes.FetchPlanDetail;

  constructor(public id: string) {}
}

export class FetchPlanDetailFailure implements Action {
  readonly type = PlanActionTypes.FetchPlanDetailFailure;

  constructor(public error: Error) {}
}

export class FetchPlanDetailSuccess implements Action {
  readonly type = PlanActionTypes.FetchPlanDetailSuccess;

  constructor(public data: RavenPlanDetail) {}
}

export class FetchPlanList implements Action {
  readonly type = PlanActionTypes.FetchPlanList;
}

export class FetchPlanListFailure implements Action {
  readonly type = PlanActionTypes.FetchPlanListFailure;

  constructor(public error: Error) {}
}

export class FetchPlanListSuccess implements Action {
  readonly type = PlanActionTypes.FetchPlanListSuccess;

  constructor(public data: RavenPlan[]) {}
}

export class OpenPlanFormDialog implements Action {
  readonly type = PlanActionTypes.OpenPlanFormDialog;

  constructor(public id: string | null) {}
}

export class RemovePlan implements Action {
  readonly type = PlanActionTypes.RemovePlan;

  constructor(public id: string) {}
}

export class RemovePlanFailure implements Action {
  readonly type = PlanActionTypes.RemovePlanFailure;

  constructor(public id: string, public error: Error) {}
}

export class RemovePlanSuccess implements Action {
  readonly type = PlanActionTypes.RemovePlanSuccess;

  constructor(public id: string) {}
}

export class SaveActivity implements Action {
  readonly type = PlanActionTypes.SaveActivity;

  constructor(public data: RavenActivity) {}
}

export class SaveActivityFailure implements Action {
  readonly type = PlanActionTypes.SaveActivityFailure;

  constructor(public error: Error) {}
}

export class SaveActivitySuccess implements Action {
  readonly type = PlanActionTypes.SaveActivitySuccess;

  constructor(public data: RavenActivityDetail) {}
}

export class SaveActivityDetail implements Action {
  readonly type = PlanActionTypes.SaveActivityDetail;

  constructor(public data: RavenActivityDetail) {}
}

export class SaveActivityDetailFailure implements Action {
  readonly type = PlanActionTypes.SaveActivityDetailFailure;

  constructor(public error: Error) {}
}

export class SaveActivityDetailSuccess implements Action {
  readonly type = PlanActionTypes.SaveActivityDetailSuccess;

  constructor(public data: RavenActivityDetail) {}
}

export class SavePlan implements Action {
  readonly type = PlanActionTypes.SavePlan;
}

export class SavePlanFailure implements Action {
  readonly type = PlanActionTypes.SavePlanFailure;

  constructor(public error: Error) {}
}

export class SavePlanSuccess implements Action {
  readonly type = PlanActionTypes.SavePlanSuccess;

  constructor(public data: RavenPlan) {}
}

export class SelectActivity implements Action {
  readonly type = PlanActionTypes.SelectActivity;

  constructor(public id: string | null) {}
}

export class UpdateSelectedActivity implements Action {
  readonly type = PlanActionTypes.UpdateSelectedActivity;

  constructor(public update: RavenActivityUpdate) {}
}

export class UpdateViewTimeRange implements Action {
  readonly type = PlanActionTypes.UpdateViewTimeRange;

  constructor(public viewTimeRange: RavenTimeRange) {}
}

export type PlanActions =
  | FetchPlanDetail
  | FetchPlanDetailFailure
  | FetchPlanDetailSuccess
  | FetchPlanList
  | FetchPlanListFailure
  | FetchPlanListSuccess
  | OpenPlanFormDialog
  | RemovePlan
  | RemovePlanFailure
  | RemovePlanSuccess
  | SaveActivity
  | SaveActivityFailure
  | SaveActivitySuccess
  | SaveActivityDetail
  | SaveActivityDetailFailure
  | SaveActivityDetailSuccess
  | SavePlan
  | SavePlanFailure
  | SavePlanSuccess
  | SelectActivity
  | UpdateSelectedActivity
  | UpdateViewTimeRange;
