/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

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
  LoadSourceWithContent =      '[source-explorer] load_source_with_content',
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

  constructor(public url: string) {}
}

export class FetchGraphDataFailure implements Action {
  readonly type = SourceExplorerActionTypes.FetchGraphDataFailure;
}

export class FetchGraphDataSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchGraphDataSuccess;
}

export class FetchInitialSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSources;

  constructor(public url: string) {}
}

export class FetchInitialSourcesFailure implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSourcesFailure;
}

export class FetchInitialSourcesSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSourcesSuccess;
}

export class FetchSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchSources;

  constructor(public url: string) {}
}

export class FetchSourcesFailure implements Action {
  readonly type = SourceExplorerActionTypes.FetchSourcesFailure;
}

export class FetchSourcesSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchSourcesSuccess;
}

// Union type of all Source Explorer actions.
export type SourceExplorerActions =
  FetchGraphData |
  FetchGraphDataFailure |
  FetchGraphDataSuccess |
  FetchInitialSources |
  FetchInitialSourcesFailure |
  FetchInitialSourcesSuccess |
  FetchSources |
  FetchSourcesFailure |
  FetchSourcesSuccess;
