/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { ActivityType, Adaptation } from '../../../../../schemas/types/ts';
import { StringTMap } from '../../shared/models';

export enum AdaptationActionTypes {
  FetchActivityTypes = '[adaptation] fetch_activity_types',
  FetchActivityTypesFailure = '[adaptation] fetch_activity_types_failure',
  FetchActivityTypesSuccess = '[adaptation] fetch_activity_types_success',
  FetchAdaptations = '[adaptation] fetch_adaptations',
  FetchAdaptationsFailure = '[adaptation] fetch_adaptations_failure',
  FetchAdaptationsSuccess = '[adaptation] fetch_adaptations_success',
}

export class FetchActivityTypes implements Action {
  readonly type = AdaptationActionTypes.FetchActivityTypes;
  constructor(public adaptationId: string) {}
}

export class FetchActivityTypesSuccess implements Action {
  readonly type = AdaptationActionTypes.FetchActivityTypesSuccess;
  constructor(public data: StringTMap<ActivityType>) {}
}

export class FetchActivityTypesFailure implements Action {
  readonly type = AdaptationActionTypes.FetchActivityTypesFailure;
  constructor(public error: Error) {}
}

export class FetchAdaptations implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptations;
}

export class FetchAdaptationsFailure implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationsFailure;
  constructor(public error: Error) {}
}

export class FetchAdaptationsSuccess implements Action {
  readonly type = AdaptationActionTypes.FetchAdaptationsSuccess;
  constructor(public data: Adaptation[]) {}
}

export type AdaptationActions =
  | FetchActivityTypes
  | FetchActivityTypesFailure
  | FetchActivityTypesSuccess
  | FetchAdaptations
  | FetchAdaptationsFailure
  | FetchAdaptationsSuccess;
