/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { ActivityType, Adaptation } from '../../shared/models';

export enum AdaptationActionTypes {
  FetchActivityTypesFailure = '[adaptation] fetch_activity_types_failure',
  FetchAdaptationsFailure = '[adaptation] fetch_adaptations_failure',
  SetActivityTypes = '[adaptation] set_activity_types',
  SetAdaptations = '[adaptation] set_adaptations',
}

export class FetchActivityTypesFailure implements Action {
  readonly type = AdaptationActionTypes.FetchActivityTypesFailure;
  constructor(public error: Error) {}
}

export class FetchAdaptationsFailure implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationsFailure;
  constructor(public error: Error) {}
}

export class SetActivityTypes implements Action {
  readonly type = AdaptationActionTypes.SetActivityTypes;
  constructor(public activityTypes: ActivityType[]) {}
}

export class SetAdaptations implements Action {
  readonly type = AdaptationActionTypes.SetAdaptations;
  constructor(public adaptations: Adaptation[]) {}
}

export type AdaptationActions =
  | FetchActivityTypesFailure
  | FetchAdaptationsFailure
  | SetActivityTypes
  | SetAdaptations;
