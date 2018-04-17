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
  RavenSource,
  StringTMap,
} from './../shared/models';

// Action Types.
export enum SourceExplorerActionTypes {
  ApplyLayout          = '[sourceExplorer] apply-layout',
  ApplyState           = '[sourceExplorer] apply-state',
  CloseEvent           = '[sourceExplorer] close_event',
  CollapseEvent        = '[sourceExplorer] collapse_event',
  ExpandEvent          = '[sourceExplorer] expand_event',
  FetchInitialSources  = '[sourceExplorer] fetch_initial_sources',
  NewSources           = '[sourceExplorer] new_sources',
  OpenEvent            = '[sourceExplorer] open_event',
  RemoveSource         = '[sourceExplorer] remove_source',
  RemoveSourceEvent    = '[sourceExplorer] remove_source_event',
  SaveState            = '[sourceExplorer] save_state',
  SelectSource         = '[sourceExplorer] select_source',
  SubBandIdAdd         = '[sourceExplorer] sub_band_id_add',
  SubBandIdRemove      = '[sourceExplorer] sub_band_id_remove',
  UpdateSourceExplorer = '[sourceExplorer] update_source_explorer',
  UpdateTreeSource     = '[sourceExplorer] update_tree_source',
}

// Actions.
export class ApplyLayout implements Action {
  readonly type = SourceExplorerActionTypes.ApplyLayout;

  constructor(public sourceUrl: string) {}
}

export class ApplyState implements Action {
  readonly type = SourceExplorerActionTypes.ApplyState;

  constructor(public sourceUrl: string) {}
}

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

export class SaveState implements Action {
  readonly type = SourceExplorerActionTypes.SaveState;

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

  constructor(public sourceIds: string[], public subBandId: string) {}
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
  ApplyLayout |
  ApplyState |
  CloseEvent |
  CollapseEvent |
  ExpandEvent |
  FetchInitialSources |
  NewSources |
  OpenEvent |
  RemoveSource |
  RemoveSourceEvent |
  SaveState |
  SelectSource |
  SubBandIdAdd |
  SubBandIdRemove |
  UpdateSourceExplorer |
  UpdateTreeSource;
