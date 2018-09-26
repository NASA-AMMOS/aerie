/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { RavenPlan } from '../../shared/models/raven-plan';

export enum PlanActionTypes {
  FetchPlanList = '[plan] fetch_plan_list',
  FetchPlanListFailure = '[plan] fetch_plan_list_failure',
  FetchPlanListSuccess = '[plan] fetch_plan_list_success',
  OpenPlanFormDialog = '[plan] open_plan_form_dialog',
  RemovePlan = '[plan] remove_plan',
  RemovePlanFailure = '[plan] remove_plan_failure',
  RemovePlanSuccess = '[plan] remove_plan_success',
  SavePlan = '[plan] save_plan',
  SavePlanFailure = '[plan] save_plan_failure',
  SavePlanSuccess = '[plan] save_plan_success',
  SelectPlan = '[plan] select_plan',
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

export class SavePlan implements Action {
  readonly type = PlanActionTypes.SavePlan;
}

export class SavePlanFailure implements Action {
  readonly type = PlanActionTypes.SavePlanFailure;

  constructor(public error: Error) {}
}

export class SavePlanSuccess implements Action {
  readonly type = PlanActionTypes.SavePlanSuccess;

  constructor(public data: RavenPlan, public isNew: boolean = false) {}
}

export class SelectPlan implements Action {
  readonly type = PlanActionTypes.SelectPlan;

  constructor(public id: string) {}
}

export type PlanActions =
  | FetchPlanList
  | FetchPlanListFailure
  | FetchPlanListSuccess
  | OpenPlanFormDialog
  | RemovePlan
  | RemovePlanFailure
  | RemovePlanSuccess
  | SavePlan
  | SavePlanFailure
  | SavePlanSuccess
  | SelectPlan;
