/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { BaseType, StringTMap } from '../../shared/models';
import { RavenEpoch } from '../models';

export enum EpochsActionTypes {
  AddEpochs = '[epochs] add_epochs',
  AppendAndReplaceEpochs = '[epochs] append_and_replace_epochs',
  FetchEpochs = '[epochs] fetch_epochs',
  RemoveEpochs = '[epochs] remove_epochs',
  SaveNewEpochFile = '[epochs] save_new_epoch_file',
  SaveNewEpochFileSuccess = '[epochs] save_new_epoch_file_success',
  SetInUseEpochByName = '[epochs] set_in_use_epoch_by_name',
  UpdateEpochData = '[epochs] update_epoch_data',
  UpdateEpochSetting = '[epochs] update_epoch_setting',
  UpdateProjectEpochs = '[epochs] update_project_epochs',
  UpdateProjectEpochsSuccess = '[epochs] update_project_epochs_success',
}

export class AddEpochs implements Action {
  readonly type = EpochsActionTypes.AddEpochs;

  constructor(public epochs: RavenEpoch[]) {}
}

export class AppendAndReplaceEpochs implements Action {
  readonly type = EpochsActionTypes.AppendAndReplaceEpochs;

  constructor(public epochs: RavenEpoch[]) {}
}

export class FetchEpochs implements Action {
  readonly type = EpochsActionTypes.FetchEpochs;

  constructor(public url: string, public replaceAction: string) {}
}

export class RemoveEpochs implements Action {
  readonly type = EpochsActionTypes.RemoveEpochs;

  constructor(public epochs: RavenEpoch[]) {}
}

export class SaveNewEpochFile implements Action {
  readonly type = EpochsActionTypes.SaveNewEpochFile;

  constructor(public filePathName: string) {}
}

export class SaveNewEpochFileSuccess implements Action {
  readonly type = EpochsActionTypes.SaveNewEpochFileSuccess;
}

export class SetInUseEpochByName implements Action {
  readonly type = EpochsActionTypes.SetInUseEpochByName;

  constructor(public epochName: string) {}
}

export class UpdateEpochData implements Action {
  readonly type = EpochsActionTypes.UpdateEpochData;

  constructor(public index: number, public data: RavenEpoch) {}
}

export class UpdateEpochSetting implements Action {
  readonly type = EpochsActionTypes.UpdateEpochSetting;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateProjectEpochs implements Action {
  readonly type = EpochsActionTypes.UpdateProjectEpochs;
}

export class UpdateProjectEpochsSuccess implements Action {
  readonly type = EpochsActionTypes.UpdateProjectEpochsSuccess;
}

export type EpochsAction =
  | AddEpochs
  | AppendAndReplaceEpochs
  | FetchEpochs
  | RemoveEpochs
  | SaveNewEpochFile
  | SaveNewEpochFileSuccess
  | SetInUseEpochByName
  | UpdateEpochData
  | UpdateEpochSetting
  | UpdateProjectEpochs
  | UpdateProjectEpochsSuccess;
