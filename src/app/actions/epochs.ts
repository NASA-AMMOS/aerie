import { Action } from '@ngrx/store';
import { RavenEpoch } from '../shared/models';

// Action Types.
export enum EpochsActionTypes {
  AddEpochs            = '[epochs] add_epochs',
  FetchEpochs          = '[epochs] fetch_epochs',
  SelectEpoch          = '[epochs] select_epoch',
}

// Actions.
export class AddEpochs implements Action {
  readonly type = EpochsActionTypes.AddEpochs;
  constructor(public epochs: RavenEpoch[]) { }
}


export class FetchEpochs implements Action {
  readonly type = EpochsActionTypes.FetchEpochs;
  constructor() {}
}

export class SelectEpoch implements Action {
  readonly type = EpochsActionTypes.SelectEpoch;
  constructor(public epoch: RavenEpoch) { }
}

// Union type of all actions.
export type EpochsAction =
  AddEpochs |
  FetchEpochs |
  SelectEpoch;
