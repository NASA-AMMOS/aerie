/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action, combineReducers } from '@ngrx/store';
import * as fromRoot from '../app-store';
import * as fromEpochs from './reducers/epochs.reducer';
import * as fromLayout from './reducers/layout.reducer';
import * as fromOutput from './reducers/output.reducer';
import * as fromSituationalAwareness from './reducers/situational-awareness.reducer';
import * as fromSourceExplorer from './reducers/source-explorer.reducer';
import * as fromTimeCursor from './reducers/time-cursor.reducer';
import * as fromTimeline from './reducers/timeline.reducer';

export interface State {
  epochs: fromEpochs.EpochsState;
  layout: fromLayout.LayoutState;
  output: fromOutput.OutputState;
  situationalAwareness: fromSituationalAwareness.SituationalAwarenessState;
  sourceExplorer: fromSourceExplorer.SourceExplorerState;
  timeCursor: fromTimeCursor.TimeCursorState;
  timeline: fromTimeline.TimelineState;
}

export interface RavenAppState extends fromRoot.AppState {
  raven: State;
}

export function reducers(state: State | undefined, action: Action) {
  return combineReducers({
    epochs: fromEpochs.reducer,
    layout: fromLayout.reducer,
    output: fromOutput.reducer,
    situationalAwareness: fromSituationalAwareness.reducer,
    sourceExplorer: fromSourceExplorer.reducer,
    timeCursor: fromTimeCursor.reducer,
    timeline: fromTimeline.reducer,
  })(state, action);
}
