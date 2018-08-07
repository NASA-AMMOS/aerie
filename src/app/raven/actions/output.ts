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
  BaseType,
  StringTMap,
 } from './../../shared/models';

// Action Types.
export enum OutputActionTypes {
  AppendData           = '[output] append_data',
  CreateOutput         = '[output] create_output',
  UpdateOutputSettings = '[output] update_output_settings',
  WriteFile            = '[output] write_file',
}

// Actions.
export class AppendData implements Action {
  readonly type = OutputActionTypes.AppendData;

  constructor(public data: string) {}
}

export class CreateOutput implements Action {
  readonly type = OutputActionTypes.CreateOutput;
}

export class UpdateOutputSettings implements Action {
  readonly type = OutputActionTypes.UpdateOutputSettings;

  constructor(public update: StringTMap<BaseType>) {}
}

export class WriteFile implements Action {
  readonly type = OutputActionTypes.WriteFile;
}

// Union type of all actions.
export type OutputAction =
  AppendData |
  CreateOutput |
  UpdateOutputSettings |
  WriteFile;
