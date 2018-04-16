/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ActionReducer,
  ActionReducerMap,
  MetaReducer,
} from '@ngrx/store';

import { environment } from './../environments/environment';
import { RouterStateUrl } from './routes';

import * as fromRouter from '@ngrx/router-store';

/**
 * storeFreeze prevents state from being mutated. When mutation occurs, an
 * exception will be thrown. This is useful during development mode to
 * ensure that none of the reducers accidentally mutates the state.
 */
import { storeFreeze } from 'ngrx-store-freeze';

/**
 * Every reducer module's default export is the reducer function itself. In
 * addition, each module should export a type or interface that describes
 * the state of the reducer plus any selector functions. The `* as`
 * notation packages up all of the exports into a single object.
 */

import * as fromConfig from './reducers/config';
import * as fromEpochs from './reducers/epochs';
import * as fromLayout from './reducers/layout';
import * as fromSourceExplorer from './reducers/source-explorer';
import * as fromTimeline from './reducers/timeline';

/**
 * As mentioned, we treat each reducer like a table in a database. This means
 * our top level state interface is just a map of keys to inner state types.
 */
export interface AppState {
  config: fromConfig.ConfigState;
  epochs: fromEpochs.EpochsState;
  layout: fromLayout.LayoutState;
  routerReducer: fromRouter.RouterReducerState<RouterStateUrl>;
  sourceExplorer: fromSourceExplorer.SourceExplorerState;
  timeline: fromTimeline.TimelineState;
}

/**
 * Our state is composed of a map of action reducer functions.
 * These reducer functions are called with each dispatched action
 * and the current or initial state and return a new immutable state.
 */
export const reducers: ActionReducerMap<AppState> = {
  config: fromConfig.reducer,
  epochs: fromEpochs.reducer,
  layout: fromLayout.reducer,
  routerReducer: fromRouter.routerReducer,
  sourceExplorer: fromSourceExplorer.reducer,
  timeline: fromTimeline.reducer,
};

/**
 * console.log all actions for debugging purposes.
 */
export function logger(reducer: ActionReducer<AppState>): ActionReducer<AppState> {
  return function(state: AppState, action: any): AppState {
    console.log('state before update', state);
    console.log('action', action);

    return reducer(state, action);
  };
}

/**
 * By default, @ngrx/store uses combineReducers with the reducer map to compose
 * the root meta-reducer. To add more meta-reducers, provide an array of meta-reducers
 * that will be composed to form the root meta-reducer.
 */
export const metaReducers: MetaReducer<AppState>[] = !environment.production
  ? [logger, storeFreeze]
  : [];
