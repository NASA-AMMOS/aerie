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
  ImportData,
  RavenSource,
  StringTMap,
} from './../shared/models';

// Action Types.
export enum SourceExplorerActionTypes {
  DeleteSource                = '[sourceExplorer] delete_source',
  DeleteSourceFailure         = '[sourceExplorer] delete_source_failure',
  DeleteSourceSuccess         = '[sourceExplorer] delete_source_success',
  FetchInitialSources         = '[sourceExplorer] fetch_initial_sources',
  ImportSource                = '[sourceExplorer] import_source',
  ImportSourceFailure         = '[sourceExplorer] import_source_failure',
  ImportSourceSuccess         = '[sourceExplorer] import_source_success',
  NewSources                  = '[sourceExplorer] new_sources',
  SourceExplorerCloseEvent    = '[sourceExplorer] source_explorer_close_event',
  SourceExplorerCollapseEvent = '[sourceExplorer] source_explorer_collapse_event',
  SourceExplorerExpandEvent   = '[sourceExplorer] source_explorer_expand_event',
  SourceExplorerOpenEvent     = '[sourceExplorer] source_explorer_open_event',
  SourceExplorerSelect        = '[sourceExplorer] source_explorer_select',
  SubBandIdAdd                = '[sourceExplorer] sub_band_id_add',
  SubBandIdRemove             = '[sourceExplorer] sub_band_id_remove',
  UpdateSourceExplorer        = '[sourceExplorer] update_source_explorer',
  UpdateTreeSource            = '[sourceExplorer] update_tree_source',
}

// Actions.
export class DeleteSource implements Action {
  readonly type = SourceExplorerActionTypes.DeleteSource;

  constructor(public source: RavenSource) {}
}

export class DeleteSourceFailure implements Action {
  readonly type = SourceExplorerActionTypes.DeleteSourceFailure;

  constructor(public source: RavenSource) {}
}

export class DeleteSourceSuccess implements Action {
  readonly type = SourceExplorerActionTypes.DeleteSourceSuccess;

  constructor(public source: RavenSource) {}
}

export class FetchInitialSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSources;
}

export class ImportSource implements Action {
  readonly type = SourceExplorerActionTypes.ImportSource;

  constructor(public importData: ImportData, public source: RavenSource) {}
}

export class ImportSourceFailure implements Action {
  readonly type = SourceExplorerActionTypes.ImportSourceFailure;
}

export class ImportSourceSuccess implements Action {
  readonly type = SourceExplorerActionTypes.ImportSourceSuccess;
}

export class NewSources implements Action {
  readonly type = SourceExplorerActionTypes.NewSources;

  constructor(public sourceId: string, public sources: RavenSource[]) {}
}

export class SourceExplorerCloseEvent implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerCloseEvent;

  constructor(public sourceId: string) {}
}

export class SourceExplorerCollapseEvent implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerCollapseEvent;

  constructor(public sourceId: string) {}
}

export class SourceExplorerExpandEvent implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerExpandEvent;

  constructor(public sourceId: string) {}
}

export class SourceExplorerOpenEvent implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerOpenEvent;

  constructor(public sourceId: string) {}
}

export class SourceExplorerSelect implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerSelect;

  constructor(public source: RavenSource) {}
}

export class SubBandIdAdd implements Action {
  readonly type = SourceExplorerActionTypes.SubBandIdAdd;

  constructor(public sourceId: string, public subBandId: string) {}
}

export class SubBandIdRemove implements Action {
  readonly type = SourceExplorerActionTypes.SubBandIdRemove;

  constructor(public sourceIds: StringTMap<string>, public subBandId: string) {}
}

export class UpdateSourceExplorer implements Action {
  readonly type = SourceExplorerActionTypes.UpdateSourceExplorer;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateTreeSource implements Action {
  readonly type = SourceExplorerActionTypes.UpdateTreeSource;

  constructor(
    public sourceId: string,
    public prop: string,
    public value: any,
  ) {}
}

// Union type of all actions.
export type SourceExplorerAction =
  DeleteSource |
  DeleteSourceFailure |
  DeleteSourceSuccess |
  FetchInitialSources |
  ImportSource |
  ImportSourceFailure |
  ImportSourceSuccess |
  NewSources |
  SourceExplorerCloseEvent |
  SourceExplorerCollapseEvent |
  SourceExplorerExpandEvent |
  SourceExplorerOpenEvent |
  SourceExplorerSelect |
  SubBandIdAdd |
  SubBandIdRemove |
  UpdateSourceExplorer |
  UpdateTreeSource;
