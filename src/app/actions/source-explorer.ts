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
  RavenActivityPoint,
  RavenBand,
  RavenSource,
  StringTMap,
} from './../shared/models';

// Action Types.
export enum SourceExplorerActionTypes {
  FetchGraphData             = '[sourceExplorer] fetch_graph_data',
  FetchGraphDataFailure      = '[sourceExplorer] fetch_graph_data_failure',
  FetchGraphDataSuccess      = '[sourceExplorer] fetch_graph_data_success',
  FetchInitialSources        = '[sourceExplorer] fetch_initial_sources',
  FetchInitialSourcesFailure = '[sourceExplorer] fetch_initial_sources_failure',
  FetchInitialSourcesSuccess = '[sourceExplorer] fetch_initial_sources_success',
  FetchSources               = '[sourceExplorer] fetch_sources',
  FetchSourcesFailure        = '[sourceExplorer] fetch_sources_failure',
  FetchSourcesSuccess        = '[sourceExplorer] fetch_sources_success',
  LoadContent                = '[sourceExplorer] load_content',
  RemoveBands                = '[sourceExplorer] remove_bands',
  SourceExplorerClose        = '[sourceExplorer] source_explorer_close',
  SourceExplorerCollapse     = '[sourceExplorer] source_explorer_collapse',
  SourceExplorerExpand       = '[sourceExplorer] source_explorer_expand',
  SourceExplorerOpen         = '[sourceExplorer] source_explorer_open',
  SourceExplorerPin          = '[sourceExplorer] source_explorer_pin',
  SourceExplorerSelect       = '[sourceExplorer] source_explorer_select',
  SourceExplorerUnpin        = '[sourceExplorer] source_explorer_unpin',
}

// Actions.
export class FetchGraphData implements Action {
  readonly type = SourceExplorerActionTypes.FetchGraphData;

  constructor(public source: RavenSource) {}
}

export class FetchGraphDataFailure implements Action {
  readonly type = SourceExplorerActionTypes.FetchGraphDataFailure;
}

export class FetchGraphDataSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchGraphDataSuccess;

  constructor(
    public source: RavenSource,
    public bands: RavenBand[],
    public bandIdToName: StringTMap<string>,
    public bandIdToPoints: StringTMap<RavenActivityPoint[]>,
  ) {}
}

export class FetchInitialSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSources;
}

export class FetchInitialSourcesFailure implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSourcesFailure;
}

export class FetchInitialSourcesSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSourcesSuccess;

  constructor(public sources: RavenSource[]) {}
}

export class FetchSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchSources;

  constructor(public source: RavenSource) {}
}

export class FetchSourcesFailure implements Action {
  readonly type = SourceExplorerActionTypes.FetchSourcesFailure;
}

export class FetchSourcesSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchSourcesSuccess;

  constructor(public source: RavenSource, public sources: RavenSource[]) {}
}

export class LoadContent implements Action {
  readonly type = SourceExplorerActionTypes.LoadContent;

  constructor(public source: RavenSource, public sources: RavenSource[]) {}
}

export class RemoveBands implements Action {
  readonly type = SourceExplorerActionTypes.RemoveBands;

  constructor(
    public source: RavenSource,
    public removeBandIds: string[],
    public removePointsBandIds: string[],
  ) {}
}

export class SourceExplorerClose implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerClose;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerCollapse implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerCollapse;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerExpand implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerExpand;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerOpen implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerOpen;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerPin implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerPin;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerSelect implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerSelect;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerUnpin implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerUnpin;

  constructor(public source: RavenSource) {}
}

// Union type of all actions.
export type SourceExplorerAction =
  FetchGraphData |
  FetchGraphDataFailure |
  FetchGraphDataSuccess |
  FetchInitialSources |
  FetchInitialSourcesFailure |
  FetchInitialSourcesSuccess |
  FetchSources |
  FetchSourcesFailure |
  FetchSourcesSuccess |
  LoadContent |
  RemoveBands |
  SourceExplorerClose |
  SourceExplorerCollapse |
  SourceExplorerExpand |
  SourceExplorerOpen |
  SourceExplorerPin |
  SourceExplorerSelect |
  SourceExplorerUnpin;
