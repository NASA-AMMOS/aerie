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
import { RavenAdaptation } from '../../shared/models/raven-adaptation';
import { RavenAdaptationDetail } from '../../shared/models/raven-adaptation-detail';

export enum AdaptationActionTypes {
  FetchAdaptation = '[adaptation] fetch_adaptation',
  FetchAdaptationFailure = '[adaptation] fetch_adaptation_failure',
  FetchAdaptationList = '[adaptation] fetch_adaptation_list',
  FetchAdaptationListFailure = '[adaptation] fetch_adaptation_list_failure',
  FetchAdaptationListSuccess = '[adaptation] fetch_adaptation_list_success',
  FetchAdaptationSuccess = '[adaptation] fetch_adaptation_success',
  OpenActivityTypeFormDialog = '[adaptation] open_activity_type_form_dialog',
  RemoveActivityType = '[adaptation] remove_activity_type',
  RemoveActivityTypeFailure = '[adaptation] remove_activity_type_failure',
  RemoveActivityTypeSuccess = '[adaptation] remove_activity_type_success',
  SaveActivityType = '[adaptation] save_activity_type',
  SaveActivityTypeFailure = '[adaptation] save_activity_type_failure',
  SaveActivityTypeSuccess = '[adaptation] save_activity_type_success',
}

export class FetchAdaptation implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptation;

  constructor(public id: string) {}
}

export class FetchAdaptationFailure implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationFailure;

  constructor(public error: Error) {}
}

export class FetchAdaptationSuccess implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationSuccess;

  constructor(public data: RavenAdaptationDetail) {}
}

export class FetchAdaptationList implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationList;
}

export class FetchAdaptationListFailure implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationListFailure;

  constructor(public error: Error) {}
}

export class FetchAdaptationListSuccess implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationListSuccess;

  constructor(public data: RavenAdaptation[]) {}
}

export class OpenActivityTypeFormDialog implements Action {
  readonly type = AdaptationActionTypes.OpenActivityTypeFormDialog;

  constructor(public id: string | null) {}
}

export class RemoveActivityType implements Action {
  readonly type = AdaptationActionTypes.RemoveActivityType;

  constructor(public id: string) {}
}

export class RemoveActivityTypeFailure implements Action {
  readonly type = AdaptationActionTypes.RemoveActivityTypeFailure;

  constructor(public id: string, public error: Error) {}
}

export class RemoveActivityTypeSuccess implements Action {
  readonly type = AdaptationActionTypes.RemoveActivityTypeSuccess;

  constructor(public id: string) {}
}

export class SaveActivityType implements Action {
  readonly type = AdaptationActionTypes.SaveActivityType;
}

export class SaveActivityTypeFailure implements Action {
  readonly type = AdaptationActionTypes.SaveActivityTypeFailure;

  constructor(public error: Error) {}
}

export class SaveActivityTypeSuccess implements Action {
  readonly type = AdaptationActionTypes.SaveActivityTypeSuccess;

  constructor(public data: RavenActivityType, public isNew: boolean = false) {}
}

export type AdaptationActions =
  | FetchAdaptation
  | FetchAdaptationFailure
  | FetchAdaptationList
  | FetchAdaptationListFailure
  | FetchAdaptationListSuccess
  | FetchAdaptationSuccess
  | OpenActivityTypeFormDialog
  | RemoveActivityType
  | RemoveActivityTypeFailure
  | RemoveActivityTypeSuccess
  | SaveActivityType
  | SaveActivityTypeFailure
  | SaveActivityTypeSuccess;
