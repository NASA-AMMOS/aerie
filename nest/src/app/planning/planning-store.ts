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
import * as fromAdaptation from './reducers/adaptation.reducer';
import * as fromLayout from './reducers/layout.reducer';
import * as fromPlan from './reducers/plan.reducer';

export interface State {
  adaptation: fromAdaptation.AdaptationState;
  layout: fromLayout.LayoutState;
  plan: fromPlan.PlanState;
}

export interface PlanningAppState extends fromRoot.AppState {
  planning: State;
}

export function reducers(state: State | undefined, action: Action) {
  return combineReducers({
    adaptation: fromAdaptation.reducer,
    layout: fromLayout.reducer,
    plan: fromPlan.reducer,
  })(state, action);
}
