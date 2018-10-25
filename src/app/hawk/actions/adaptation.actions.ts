/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { RavenAdaptation } from '../../shared/models/raven-adaptation';

export enum AdaptationActionTypes {
  FetchAdaptationList = '[adaptation] fetch_adaptation_list',
  FetchAdaptationListFailure = '[adaptation] fetch_adaptation_list_failure',
  FetchAdaptationListSuccess = '[adaptation] fetch_adaptation_list_success',
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

export type AdaptationActions =
  | FetchAdaptationList
  | FetchAdaptationListFailure
  | FetchAdaptationListSuccess;
