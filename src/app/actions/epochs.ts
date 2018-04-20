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
  RavenEpoch,
} from './../shared/models';

// Action Types.
export enum EpochsActionTypes {
  AddEpochs                = '[epochs] add_epochs',
  ChangeDayCode            = '[epochs] change_day_code',
  ChangeEarthSecToEpochSec = '[epochs] change_earth_sec_to_epoch_sec',
  FetchEpochs              = '[epochs] fetch_epochs',
  SelectEpoch              = '[epochs] select_epoch',
}

// Actions.
export class AddEpochs implements Action {
  readonly type = EpochsActionTypes.AddEpochs;

  constructor(public epochs: RavenEpoch[]) {}
}

export class ChangeDayCode implements Action {
  readonly type = EpochsActionTypes.ChangeDayCode;

  constructor(public code: string) {}
}

export class ChangeEarthSecToEpochSec implements Action {
  readonly type = EpochsActionTypes.ChangeEarthSecToEpochSec;

  constructor(public earthSecToEpochSec: number) {}
}

export class FetchEpochs implements Action {
  readonly type = EpochsActionTypes.FetchEpochs;

  constructor(public url: string) {}
}

export class SelectEpoch implements Action {
  readonly type = EpochsActionTypes.SelectEpoch;

  constructor(public epoch: RavenEpoch) {}
}

// Union type of all actions.
export type EpochsAction =
  AddEpochs |
  ChangeDayCode |
  ChangeEarthSecToEpochSec |
  FetchEpochs |
  SelectEpoch;
