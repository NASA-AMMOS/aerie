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
} from './../models';

// Action Types.
export enum SourceExplorerActionTypes {
  FetchGraphData =             '[source-explorer] fetch_graph_data',
  FetchGraphDataFailure =      '[source-explorer] fetch_graph_data_failure',
  FetchGraphDataSuccess =      '[source-explorer] fetch_graph_data_success',
  FetchInitialSources =        '[source-explorer] fetch_initial_sources',
  FetchInitialSourcesFailure = '[source-explorer] fetch_initial_sources_failure',
  FetchInitialSourcesSuccess = '[source-explorer] fetch_initial_sources_success',
  FetchSources =               '[source-explorer] fetch_sources',
  FetchSourcesFailure =        '[source-explorer] fetch_sources_failure',
  FetchSourcesSuccess =        '[source-explorer] fetch_sources_success',
  LoadContent =                '[source-explorer] load_content',
  RemoveBands =                '[source-explorer] remove_bands',
  SourceExplorerCollapse =     '[source-explorer] source_explorer_collapse',
  SourceExplorerExpand =       '[source-explorer] source_explorer_expand',
  SourceExplorerClose =        '[source-explorer] source_explorer_close',
  SourceExplorerOpen =         '[source-explorer] source_explorer_open',
  SourceExplorerPin =          '[source-explorer] source_explorer_pin',
  SourceExplorerUnpin =        '[source-explorer] source_explorer_unpin',
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
    public bandIdsToPoints: StringTMap<RavenActivityPoint[]>,
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

export class SourceExplorerCollapse implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerCollapse;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerExpand implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerExpand;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerClose implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerClose;

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
  SourceExplorerCollapse |
  SourceExplorerExpand |
  SourceExplorerClose |
  SourceExplorerOpen |
  SourceExplorerPin |
  SourceExplorerUnpin;
