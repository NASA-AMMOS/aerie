/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import {
  BaseType,
  RavenApplyLayoutUpdate,
  RavenCustomFilterSource,
  RavenExpandableSource,
  RavenFile,
  RavenFilterSource,
  RavenGraphableFilterSource,
  RavenPin,
  RavenSource,
  SourceFilter,
  StringTMap,
} from '../models';

export const addCustomFilter = createAction(
  '[raven-source-explorer] add_custom_filter',
  props<{ sourceId: string; label: string; customFilter: string }>(),
);

export const addCustomGraph = createAction(
  '[raven-source-explorer] add_custom_graph',
  props<{ sourceId: string; label: string; customFilter: string }>(),
);

export const addFilter = createAction(
  '[raven-source-explorer] add_filter',
  props<{ source: RavenFilterSource }>(),
);

export const addGraphableFilter = createAction(
  '[raven-source-explorer] add_graphable_filter',
  props<{ source: RavenGraphableFilterSource }>(),
);

export const applyCurrentState = createAction(
  '[raven-source-explorer] apply_current_state',
);

export const applyLayout = createAction(
  '[raven-source-explorer] apply_layout',
  props<{ update: RavenApplyLayoutUpdate }>(),
);

export const applyLayoutToSources = createAction(
  '[raven-source-explorer] apply_layout_to_sources',
  props<{
    layoutSourceUrl: string;
    layoutSourceId: string;
    sourcePaths: string[];
  }>(),
);

export const applyState = createAction(
  '[raven-source-explorer] apply_state',
  props<{ sourceUrl: string; sourceId: string }>(),
);

export const applyStateOrLayoutSuccess = createAction(
  '[raven-source-explorer] apply_state_or_layout_success',
);

export const closeEvent = createAction(
  '[raven-source-explorer] close_event',
  props<{ sourceId: string }>(),
);

export const collapseEvent = createAction(
  '[raven-source-explorer] collapse_event',
  props<{ sourceId: string }>(),
);

export const expandEvent = createAction(
  '[raven-source-explorer] expand_event',
  props<{ sourceId: string }>(),
);

export const fetchInitialSources = createAction(
  '[raven-source-explorer] fetch_initial_sources',
);

export const fetchNewSources = createAction(
  '[raven-source-explorer] fetch_new_sources',
  props<{ sourceId: string; sourceUrl: string }>(),
);

export const folderAdd = createAction(
  '[raven-source-explorer] folder_add',
  props<{ folder: RavenExpandableSource }>(),
);

export const folderAddFailure = createAction(
  '[raven-source-explorer] folder_add_failure',
);

export const folderAddSuccess = createAction(
  '[raven-source-explorer] folder_add_success',
);

export const graphAgainEvent = createAction(
  '[raven-source-explorer] graph_again_event',
  props<{ sourceId: string }>(),
);

export const graphCustomSource = createAction(
  '[raven-source-explorer] graph_custom_source',
  props<{ sourceId: string; label: string; filter: string }>(),
);

export const importFile = createAction(
  '[raven-source-explorer] import_file',
  props<{ source: RavenSource; file: RavenFile }>(),
);

export const importFileFailure = createAction(
  '[raven-source-explorer] import_file_failure',
);

export const importFileSuccess = createAction(
  '[raven-source-explorer] import_file_success',
);

export const loadErrorsAdd = createAction(
  '[raven-source-explorer] load_errors_add',
  props<{ sourceIds: string[] }>(),
);

export const loadErrorsDisplay = createAction(
  '[raven-source-explorer] load_errors_display',
);

export const newSources = createAction(
  '[raven-source-explorer] new_sources',
  props<{ sourceId: string; sources: RavenSource[] }>(),
);

export const openEvent = createAction(
  '[raven-source-explorer] open_event',
  props<{ sourceId: string }>(),
);

export const pinAdd = createAction(
  '[raven-source-explorer] pin_add',
  props<{ pin: RavenPin }>(),
);

export const pinRemove = createAction(
  '[raven-source-explorer] pin_remove',
  props<{ sourceId: string }>(),
);

export const pinRename = createAction(
  '[raven-source-explorer] pin_rename',
  props<{ sourceId: string; newName: string }>(),
);

export const removeCustomFilter = createAction(
  '[raven-source-explorer] remove_custom_filter',
  props<{ sourceId: string; label: string }>(),
);

export const removeFilter = createAction(
  '[raven-source-explorer] remove_filter',
  props<{ source: RavenFilterSource }>(),
);

export const removeGraphableFilter = createAction(
  '[raven-source-explorer] remove_graphable_filter',
  props<{ source: RavenGraphableFilterSource }>(),
);

export const removeSource = createAction(
  '[raven-source-explorer] remove_source',
  props<{ sourceId: string }>(),
);

export const removeSourceEvent = createAction(
  '[raven-source-explorer] remove_source_event',
  props<{ source: RavenSource }>(),
);

export const saveState = createAction(
  '[raven-source-explorer] save_state',
  props<{ source: RavenSource; name: string }>(),
);

export const selectSource = createAction(
  '[raven-source-explorer] select_source',
  props<{ sourceId: string }>(),
);

export const setCustomFilter = createAction(
  '[raven-source-explorer] set_custom_filter',
  props<{ source: RavenCustomFilterSource; filter: string }>(),
);

export const setCustomFilterSubBandId = createAction(
  '[raven-source-explorer] set_custom_filter_sub_band_id',
  props<{ sourceId: string; label: string; subBandId: string }>(),
);

export const subBandIdAdd = createAction(
  '[raven-source-explorer] sub_band_id_add',
  props<{ sourceId: string; subBandId: string }>(),
);

export const subBandIdRemove = createAction(
  '[raven-source-explorer] sub_band_id_add_remove',
  props<{ sourceIds: string[]; subBandId: string }>(),
);

export const updateCurrentState = createAction(
  '[raven-source-explorer] update_current_state',
);

export const updateGraphAfterFilterAdd = createAction(
  '[raven-source-explorer] update_graph_after_filter_add',
  props<{ sourceId: string }>(),
);

export const updateGraphAfterFilterRemove = createAction(
  '[raven-source-explorer] update_graph_after_filter_remove',
  props<{ sourceId: string }>(),
);

export const updateSourceExplorer = createAction(
  '[raven-source-explorer] update_source_explorer',
  props<{ update: StringTMap<BaseType> }>(),
);

export const updateSourceFilter = createAction(
  '[raven-source-explorer] update_source_filter',
  props<{ sourceFilter: SourceFilter }>(),
);

export const updateTreeSource = createAction(
  '[raven-source-explorer] update_tree_source',
  props<{ sourceId: string; update: StringTMap<BaseType> }>(),
);
