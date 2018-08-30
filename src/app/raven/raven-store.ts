/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ActionReducerMap } from '@ngrx/store';
import * as fromRoot from '../app-store';

/**
 * Every reducer module's default export is the reducer function itself. In
 * addition, each module should export a type or interface that describes
 * the state of the reducer plus any selector functions. The `* as`
 * notation packages up all of the exports into a single object.
 */

import * as fromEpochs from './reducers/epochs';
import * as fromLayout from './reducers/layout';
import * as fromOutput from './reducers/output';
import * as fromSourceExplorer from './reducers/source-explorer';
import * as fromTimeCursor from './reducers/time-cursor';
import * as fromTimeline from './reducers/timeline';

/**
 * As mentioned, we treat each reducer like a table in a database. This means
 * our top level state interface is just a map of keys to inner state types.
 */
export interface State {
  epochs: fromEpochs.EpochsState;
  layout: fromLayout.LayoutState;
  output: fromOutput.OutputState;
  sourceExplorer: fromSourceExplorer.SourceExplorerState;
  timeCursor: fromTimeCursor.TimeCursorState;
  timeline: fromTimeline.TimelineState;
}

/**
 * Our state is composed of a map of action reducer functions.
 * These reducer functions are called with each dispatched action
 * and the current or initial state and return a new immutable state.
 */
export const reducers: ActionReducerMap<State> = {
  epochs: fromEpochs.reducer,
  layout: fromLayout.reducer,
  output: fromOutput.reducer,
  sourceExplorer: fromSourceExplorer.reducer,
  timeCursor: fromTimeCursor.reducer,
  timeline: fromTimeline.reducer,
};

/**
 * Export a namespaced state for this feature reducer.
 */
export interface RavenAppState extends fromRoot.AppState {
  raven: State;
}
