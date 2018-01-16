/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

import { RavenSource } from './../models';

// Action Types.
export enum SourceExplorerActionTypes {
  FetchInitialSources =                  '[source-explorer] fetch_initial_sources',
  FetchInitialSourcesSuccess =           '[source-explorer] fetch_initial_sources_success',
  FetchSourcesSuccess =                  '[source-explorer] fetch_sources_success',
  SourceExplorerCollapse =               '[source-explorer] source_explorer_collapse',
  SourceExplorerExpand =                 '[source-explorer] source_explorer_expand',
  SourceExplorerExpandWithFetchSources = '[source-explorer] source_explorer_expand_with_fetch_sources',
  SourceExplorerExpandWithLoadContent =  '[source-explorer] source_explorer_expand_with_load_content',
  SourceExplorerClose =                  '[source-explorer] source_explorer_close',
  SourceExplorerCloseWithRemoveBands =   '[source-explorer] source_explorer_close_with_remove_bands',
  SourceExplorerOpen =                   '[source-explorer] source_explorer_open',
  SourceExplorerOpenWithFetchGraphData = '[source-explorer] source_explorer_open_with_fetch_graph_data',
  SourceExplorerPin =                    '[source-explorer] source_explorer_pin',
  SourceExplorerUnpin =                  '[source-explorer] source_explorer_unpin',
}

// Actions.
export class FetchInitialSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSources;
}

export class FetchInitialSourcesSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchInitialSourcesSuccess;

  constructor(public sources: RavenSource[]) {}
}

export class FetchSourcesSuccess implements Action {
  readonly type = SourceExplorerActionTypes.FetchSourcesSuccess;

  constructor(public source: RavenSource, public sources: RavenSource[]) {}
}

export class SourceExplorerCollapse implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerCollapse;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerExpand implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerExpand;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerExpandWithFetchSources implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerExpandWithFetchSources;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerExpandWithLoadContent implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerExpandWithLoadContent;

  constructor(public source: RavenSource, public sources: RavenSource[]) {}
}

export class SourceExplorerClose implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerClose;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerCloseWithRemoveBands implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerCloseWithRemoveBands;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerOpen implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerOpen;

  constructor(public source: RavenSource) {}
}

export class SourceExplorerOpenWithFetchGraphData implements Action {
  readonly type = SourceExplorerActionTypes.SourceExplorerOpenWithFetchGraphData;

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
  FetchInitialSources |
  FetchInitialSourcesSuccess |
  FetchSourcesSuccess |
  SourceExplorerCollapse |
  SourceExplorerExpand |
  SourceExplorerExpandWithFetchSources |
  SourceExplorerExpandWithLoadContent |
  SourceExplorerClose |
  SourceExplorerOpen |
  SourceExplorerOpenWithFetchGraphData |
  SourceExplorerPin |
  SourceExplorerUnpin;
