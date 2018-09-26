/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { RavenActivityType } from '../../shared/models/raven-activity-type';

export enum ActivityTypeActionTypes {
  FetchActivityTypeList = '[activity_type] fetch_activity_type_list',
  FetchActivityTypeListFailure = '[activity_type] fetch_activity_type_list_failure',
  FetchActivityTypeListSuccess = '[activity_type] fetch_activity_type_list_success',
  OpenActivityTypeFormDialog = '[activity_type] open_activity_type_form_dialog',
  RemoveActivityType = '[activity_type] remove_activity_type',
  RemoveActivityTypeFailure = '[activity_type] remove_activity_type_failure',
  RemoveActivityTypeSuccess = '[activity_type] remove_activity_type_success',
  SaveActivityType = '[activity_type] save_activity_type',
  SaveActivityTypeFailure = '[activity_type] save_activity_type_failure',
  SaveActivityTypeSuccess = '[activity_type] save_activity_type_success',
}

export class FetchActivityTypeList implements Action {
  readonly type = ActivityTypeActionTypes.FetchActivityTypeList;
}

export class FetchActivityTypeListFailure implements Action {
  readonly type = ActivityTypeActionTypes.FetchActivityTypeListFailure;

  constructor(public error: Error) {}
}

export class FetchActivityTypeListSuccess implements Action {
  readonly type = ActivityTypeActionTypes.FetchActivityTypeListSuccess;

  constructor(public data: RavenActivityType[]) {}
}

export class OpenActivityTypeFormDialog implements Action {
  readonly type = ActivityTypeActionTypes.OpenActivityTypeFormDialog;

  constructor(public id: string | null) {}
}

export class RemoveActivityType implements Action {
  readonly type = ActivityTypeActionTypes.RemoveActivityType;

  constructor(public id: string) {}
}

export class RemoveActivityTypeFailure implements Action {
  readonly type = ActivityTypeActionTypes.RemoveActivityTypeFailure;

  constructor(public id: string, public error: Error) {}
}

export class RemoveActivityTypeSuccess implements Action {
  readonly type = ActivityTypeActionTypes.RemoveActivityTypeSuccess;

  constructor(public id: string) {}
}

export class SaveActivityType implements Action {
  readonly type = ActivityTypeActionTypes.SaveActivityType;
}

export class SaveActivityTypeFailure implements Action {
  readonly type = ActivityTypeActionTypes.SaveActivityTypeFailure;

  constructor(public error: Error) {}
}

export class SaveActivityTypeSuccess implements Action {
  readonly type = ActivityTypeActionTypes.SaveActivityTypeSuccess;

  constructor(public data: RavenActivityType, public isNew: boolean = false) {}
}

export type ActivityTypeActions =
  | FetchActivityTypeList
  | FetchActivityTypeListFailure
  | FetchActivityTypeListSuccess
  | OpenActivityTypeFormDialog
  | RemoveActivityType
  | RemoveActivityTypeFailure
  | RemoveActivityTypeSuccess
  | SaveActivityType
  | SaveActivityTypeFailure
  | SaveActivityTypeSuccess;
