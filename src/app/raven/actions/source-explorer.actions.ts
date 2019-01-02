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
  RavenApplyLayoutUpdate,
  RavenCustomFilterSource,
  RavenFile,
  RavenFilterSource,
  RavenGraphableFilterSource,
  RavenPin,
  RavenSource,
  StringTMap,
} from '../../shared/models';

// Action Types.
export enum SourceExplorerActionTypes {
  AddCustomFilter = '[sourceExplorer] add_custom_filter',
  AddCustomGraph = '[sourceExplorer] add_custom_graph',
  AddFilter = '[sourceExplorer] add_filter',
  AddGraphableFilter = '[sourceExplorer] add_graphable_filter',
  ApplyLayout = '[sourceExplorer] apply-layout',
  ApplyLayoutWithPins = '[sourceExplorer] apply-layout-with-pins',
  ApplyState = '[sourceExplorer] apply-state',
  ApplyStateOrLayoutSuccess = '[sourceExplorer] apply-state-or-layout-success',
  CloseEvent = '[sourceExplorer] close_event',
  CollapseEvent = '[sourceExplorer] collapse_event',
  ExpandEvent = '[sourceExplorer] expand_event',
  FetchInitialSources = '[sourceExplorer] fetch_initial_sources',
  FetchNewSources = '[sourceExplorer] fetch_new_sources',
  GraphAgainEvent = '[sourceExplorer] graph-again-event',
  GraphCustomSource = '[sourceExplorer] graph_custom_source',
  ImportFile = '[sourceExplorer] import_file',
  ImportFileFailure = '[sourceExplorer] import_file_failure',
  ImportFileSuccess = '[sourceExplorer] import_file_success',
  LoadErrorsAdd = '[sourceExplorer] load_errors_add',
  LoadErrorsDisplay = '[sourceExplorer] load_errors_display',
  NewSources = '[sourceExplorer] new_sources',
  OpenEvent = '[sourceExplorer] open_event',
  PinAdd = '[sourceExplorer] pin_add',
  PinRemove = '[sourceExplorer] pin_remove',
  PinRename = '[sourceExplorer] pin_rename',
  RemoveCustomFilter = '[sourceExplorer] remove_custom_filter',
  RemoveFilter = '[sourceExplorer] remove_filter',
  RemoveGraphableFilter = '[sourceExplorer] remove_graphable_filter',
  RemoveSource = '[sourceExplorer] remove_source',
  RemoveSourceEvent = '[sourceExplorer] remove_source_event',
  SaveState = '[sourceExplorer] save_state',
  SelectSource = '[sourceExplorer] select_source',
  SetCustomFilter = '[sourceExplorer] set_custom_filter',
  SetCustomFilterSubBandId = '[sourceExplorer] set_custom_filter_sub_band_id',
  SubBandIdAdd = '[sourceExplorer] sub_band_id_add',
  SubBandIdRemove = '[sourceExplorer] sub_band_id_remove',
  UpdateGraphAfterFilterAdd = '[sourceExplorer] update_graph_after_filter_add',
  UpdateGraphAfterFilterRemove = '[sourceExplorer] update_graph_after_filter_remove',
  UpdateSourceExplorer = '[sourceExplorer] update_source_explorer',
  UpdateState = '[sourceExplorer] update_state',
  UpdateTreeSource = '[sourceExplorer] update_tree_source',
}

// Actions.
export class AddCustomFilter implements Action {
  readonly type = SourceExplorerActionTypes.AddCustomFilter;

  constructor(
    public sourceId: string,
    public label: string,
    public customFilter: string,
  ) {}
}

export class AddCustomGraph implements Action {
  readonly type = SourceExplorerActionTypes.AddCustomGraph;

  constructor(
    public sourceId: string,
    public label: string,
    public customFilter: string,
  ) {}
}

export class AddFilter implements Action {
  readonly type = SourceExplorerActionTypes.AddFilter;

  constructor(public source: RavenFilterSource) {}
}

export class AddGraphableFilter implements Action {
  readonly type = SourceExplorerActionTypes.AddGraphableFilter;

  constructor(public source: RavenGraphableFilterSource) {}
}

export class ApplyLayout implements Action {
  readonly type = SourceExplorerActionTypes.ApplyLayout;

  constructor(public update: RavenApplyLayoutUpdate) {}
}

export class ApplyLayoutWithPins implements Action {
  readonly type = SourceExplorerActionTypes.ApplyLayoutWithPins;

  constructor(public update: RavenApplyLayoutUpdate) {}
}

export class ApplyState implements Action {
  readonly type = SourceExplorerActionTypes.ApplyState;

  constructor(public sourceUrl: string, public sourceId: string) {}
}

export class ApplyStateOrLayoutSuccess implements Action {
  readonly type = SourceExplorerActionTypes.ApplyStateOrLayoutSuccess;
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

export class FetchNewSources implements Action {
  readonly type = SourceExplorerActionTypes.FetchNewSources;

  constructor(public sourceId: string, public sourceUrl: string) {}
}

export class GraphAgainEvent implements Action {
  readonly type = SourceExplorerActionTypes.GraphAgainEvent;

  constructor(public sourceId: string) {}
}

export class GraphCustomSource implements Action {
  readonly type = SourceExplorerActionTypes.GraphCustomSource;

  constructor(
    public sourceId: string,
    public label: string,
    public filter: string,
  ) {}
}

export class ImportFile implements Action {
  readonly type = SourceExplorerActionTypes.ImportFile;

  constructor(public source: RavenSource, public file: RavenFile) {}
}

export class ImportFileFailure implements Action {
  readonly type = SourceExplorerActionTypes.ImportFileFailure;
}

export class ImportFileSuccess implements Action {
  readonly type = SourceExplorerActionTypes.ImportFileSuccess;
}

export class LoadErrorsAdd implements Action {
  readonly type = SourceExplorerActionTypes.LoadErrorsAdd;

  constructor(public sourceIds: string[]) {}
}

export class LoadErrorsDisplay implements Action {
  readonly type = SourceExplorerActionTypes.LoadErrorsDisplay;
}

export class NewSources implements Action {
  readonly type = SourceExplorerActionTypes.NewSources;

  constructor(public sourceId: string, public sources: RavenSource[]) {}
}

export class OpenEvent implements Action {
  readonly type = SourceExplorerActionTypes.OpenEvent;

  constructor(public sourceId: string) {}
}

export class PinAdd implements Action {
  readonly type = SourceExplorerActionTypes.PinAdd;

  constructor(public pin: RavenPin) {}
}

export class PinRemove implements Action {
  readonly type = SourceExplorerActionTypes.PinRemove;

  constructor(public sourceId: string) {}
}

export class PinRename implements Action {
  readonly type = SourceExplorerActionTypes.PinRename;

  constructor(public sourceId: string, public newName: string) {}
}

export class RemoveCustomFilter implements Action {
  readonly type = SourceExplorerActionTypes.RemoveCustomFilter;

  constructor(public sourceId: string, public label: string) {}
}

export class RemoveFilter implements Action {
  readonly type = SourceExplorerActionTypes.RemoveFilter;

  constructor(public source: RavenFilterSource) {}
}

export class RemoveGraphableFilter implements Action {
  readonly type = SourceExplorerActionTypes.RemoveGraphableFilter;

  constructor(public source: RavenGraphableFilterSource) {}
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

export class SetCustomFilter implements Action {
  readonly type = SourceExplorerActionTypes.SetCustomFilter;

  constructor(public source: RavenCustomFilterSource, public filter: string) {}
}

export class SetCustomFilterSubBandId implements Action {
  readonly type = SourceExplorerActionTypes.SetCustomFilterSubBandId;

  constructor(
    public sourceId: string,
    public label: string,
    public subBandId: string,
  ) {}
}

export class SubBandIdAdd implements Action {
  readonly type = SourceExplorerActionTypes.SubBandIdAdd;

  constructor(public sourceId: string, public subBandId: string) {}
}

export class SubBandIdRemove implements Action {
  readonly type = SourceExplorerActionTypes.SubBandIdRemove;

  constructor(public sourceIds: string[], public subBandId: string) {}
}

export class UpdateGraphAfterFilterAdd implements Action {
  readonly type = SourceExplorerActionTypes.UpdateGraphAfterFilterAdd;

  constructor(public sourceId: string) {}
}

export class UpdateGraphAfterFilterRemove implements Action {
  readonly type = SourceExplorerActionTypes.UpdateGraphAfterFilterRemove;

  constructor(public sourceId: string) {}
}

export class UpdateSourceExplorer implements Action {
  readonly type = SourceExplorerActionTypes.UpdateSourceExplorer;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateState implements Action {
  readonly type = SourceExplorerActionTypes.UpdateState;
}

export class UpdateTreeSource implements Action {
  readonly type = SourceExplorerActionTypes.UpdateTreeSource;

  constructor(public sourceId: string, public update: StringTMap<BaseType>) {}
}

// Union type of all actions.
export type SourceExplorerAction =
  | AddCustomFilter
  | AddCustomGraph
  | AddFilter
  | AddGraphableFilter
  | ApplyLayout
  | ApplyLayoutWithPins
  | ApplyState
  | ApplyStateOrLayoutSuccess
  | CloseEvent
  | CollapseEvent
  | ExpandEvent
  | FetchInitialSources
  | FetchNewSources
  | GraphAgainEvent
  | GraphCustomSource
  | ImportFile
  | ImportFileFailure
  | ImportFileSuccess
  | LoadErrorsAdd
  | LoadErrorsDisplay
  | NewSources
  | OpenEvent
  | PinAdd
  | PinRemove
  | PinRename
  | RemoveCustomFilter
  | RemoveFilter
  | RemoveGraphableFilter
  | RemoveSource
  | RemoveSourceEvent
  | SaveState
  | SelectSource
  | SetCustomFilter
  | SetCustomFilterSubBandId
  | SubBandIdAdd
  | SubBandIdRemove
  | UpdateGraphAfterFilterAdd
  | UpdateGraphAfterFilterRemove
  | UpdateSourceExplorer
  | UpdateState
  | UpdateTreeSource;
