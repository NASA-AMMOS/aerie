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
  CloseEvent           = '[sourceExplorer] close_event',
  CollapseEvent        = '[sourceExplorer] collapse_event',
  ExpandEvent          = '[sourceExplorer] expand_event',
  FetchInitialSources  = '[sourceExplorer] fetch_initial_sources',
  ImportSourceEvent    = '[sourceExplorer] import_source_event',
  ImportSourceFailure  = '[sourceExplorer] import_source_failure',
  ImportSourceSuccess  = '[sourceExplorer] import_source_success',
  LoadFromSource       = '[sourceExplorer] load_from_source',
  NewSources           = '[sourceExplorer] new_sources',
  OpenEvent            = '[sourceExplorer] open_event',
  RemoveSource         = '[sourceExplorer] remove_source',
  RemoveSourceEvent    = '[sourceExplorer] remove_source_event',
  SaveToSource         = '[sourceExplorer] save_to_source',
  SelectSource         = '[sourceExplorer] select_source',
  SubBandIdAdd         = '[sourceExplorer] sub_band_id_add',
  SubBandIdRemove      = '[sourceExplorer] sub_band_id_remove',
  UpdateBranch         = '[sourceExplorer] update_branch',
  UpdateSourceExplorer = '[sourceExplorer] update_source_explorer',
  UpdateTreeSource     = '[sourceExplorer] update_tree_source',
}

// Actions.
export class CloseEvent implements Action {
  readonly type = SourceExplorerActionTypes.CloseEvent;

  constructor(public sourceId: string) {}
}

export class CollapseEvent implements Action {
  readonly type = SourceExplorerActionTypes.CollapseEvent;

  constructor(public sourceId: string) {}
}

export class ExpandEvent implements Action {
  readonly type = SourceExplorerActionTypes.ExpandEvent;

  constructor(public sourceId: string) {}
}

export class FetchInitialSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSources;
}

export class LoadFromSource implements Action {
  readonly type = SourceExplorerActionTypes.LoadFromSource;

  constructor(public sourceUrl: string) {}
}

export class ImportSourceEvent implements Action {
  readonly type = SourceExplorerActionTypes.ImportSourceEvent;

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

export class OpenEvent implements Action {
  readonly type = SourceExplorerActionTypes.OpenEvent;

  constructor(public sourceId: string) {}
}

export class RemoveSource implements Action {
  readonly type = SourceExplorerActionTypes.RemoveSource;

  constructor(public sourceId: string) {}
}

export class RemoveSourceEvent implements Action {
  readonly type = SourceExplorerActionTypes.RemoveSourceEvent;

  constructor(public source: RavenSource) {}
}

export class SaveToSource implements Action {
  readonly type = SourceExplorerActionTypes.SaveToSource;

  constructor(public source: RavenSource, public name: string) {}
}

export class SelectSource implements Action {
  readonly type = SourceExplorerActionTypes.SelectSource;

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

export class UpdateBranch implements Action {
  readonly type = SourceExplorerActionTypes.UpdateBranch;

  constructor(public sourceUrl: string, public sourceId: string) {}
}
export class UpdateSourceExplorer implements Action {
  readonly type = SourceExplorerActionTypes.UpdateSourceExplorer;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateTreeSource implements Action {
  readonly type = SourceExplorerActionTypes.UpdateTreeSource;

  constructor(public sourceId: string, public update: StringTMap<BaseType>) {}
}

// Union type of all actions.
export type SourceExplorerAction =
  CloseEvent |
  CollapseEvent |
  ExpandEvent |
  FetchInitialSources |
  ImportSourceEvent |
  ImportSourceFailure |
  ImportSourceSuccess |
  LoadFromSource |
  NewSources |
  OpenEvent |
  RemoveSource |
  RemoveSourceEvent |
  SaveToSource |
  SelectSource |
  SubBandIdAdd |
  SubBandIdRemove |
  UpdateBranch |
  UpdateSourceExplorer |
  UpdateTreeSource;
