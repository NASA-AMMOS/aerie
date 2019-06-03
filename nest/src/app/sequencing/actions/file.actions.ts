/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

export enum FileActionTypes {
  CreateTab = '[file] create_tab',
  CloseTab = '[file] remove_tab',
  SwitchTab = '[file] switch_tab',
  UpdateTab = '[file] update_tab',
}

export class CreateTab implements Action {
  readonly type = FileActionTypes.CreateTab;
}

export class CloseTab implements Action {
  readonly type = FileActionTypes.CloseTab;

  constructor(public docIdToClose: string) {}
}

export class SwitchTab implements Action {
  readonly type = FileActionTypes.SwitchTab;

  constructor(public switchToId: string) {}
}

export class UpdateTab implements Action {
  readonly type = FileActionTypes.UpdateTab;

  constructor(public docIdToUpdate: string, public text: string) {}
}

export type FileActions = CreateTab | CloseTab | SwitchTab | UpdateTab;
