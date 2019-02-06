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
import * as fromAdaptation from './reducers/adaptation.reducer';
import * as fromPlan from './reducers/plan.reducer';

export interface State {
  adaptation: fromAdaptation.AdaptationState;
  plan: fromPlan.PlanState;
}

export const reducers: ActionReducerMap<State> = {
  adaptation: fromAdaptation.reducer,
  plan: fromPlan.reducer,
};

export interface HawkAppState extends fromRoot.AppState {
  hawk: State;
}
