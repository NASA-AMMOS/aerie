/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { SequenceFile } from '../../../../../sequencing/src/models';

export enum FileActionTypes {
  AddEditor = '[sequencing-file] add_editor',
  CreateTab = '[sequencing-file] create_tab',
  CloseTab = '[sequencing-file] remove_tab',
  FetchChildren = '[sequencing-file] fetch_children',
  FetchChildrenFailure = '[sequencing-file] fetch_children_failure',
  SwitchTab = '[sequencing-file] switch_tab',
  UpdateChildren = '[sequencing-file] update_children',
  UpdateTab = '[sequencing-file] update_tab',
}

export class AddEditor implements Action {
  readonly type = FileActionTypes.AddEditor;
}

export class CreateTab implements Action {
  readonly type = FileActionTypes.CreateTab;

  constructor(public editorId: string, public id?: string) {}
}

export class CloseTab implements Action {
  readonly type = FileActionTypes.CloseTab;

  constructor(public docIdToClose: string, public editorId: string) {}
}

export class FetchChildren implements Action {
  readonly type = FileActionTypes.FetchChildren;
  constructor(public parentId: string) {}
}

export class FetchChildrenFailure implements Action {
  readonly type = FileActionTypes.FetchChildrenFailure;
  constructor(public error: Error) {}
}

export class SwitchTab implements Action {
  readonly type = FileActionTypes.SwitchTab;

  constructor(public switchToId: string, public editorId: string) {}
}

export class UpdateChildren implements Action {
  readonly type = FileActionTypes.UpdateChildren;
  constructor(public parentId: string, public children: SequenceFile[]) {}
}

export class UpdateTab implements Action {
  readonly type = FileActionTypes.UpdateTab;

  constructor(
    public docIdToUpdate: string,
    public text: string,
    public editorId: string,
  ) {}
}

export type FileActions =
  | AddEditor
  | CreateTab
  | CloseTab
  | FetchChildren
  | FetchChildrenFailure
  | SwitchTab
  | UpdateChildren
  | UpdateTab;
