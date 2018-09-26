/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createFeatureSelector, createSelector } from '@ngrx/store';
import { RavenActivityType } from '../../shared/models/raven-activity-type';

import {
  ActivityTypeActions,
  ActivityTypeActionTypes,
  RemoveActivityType,
  SaveActivityTypeSuccess,
} from '../actions/activity-type.actions';

import { State } from '../hawk-store';

/**
 * Schema for ActivityTypeState
 */
export interface ActivityTypeState {
  activityTypes: RavenActivityType[];
}

export const initialState: ActivityTypeState = {
  activityTypes: [],
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: ActivityTypeState = initialState,
  action: ActivityTypeActions,
): ActivityTypeState {
  switch (action.type) {
    case ActivityTypeActionTypes.FetchActivityTypeListSuccess:
      return { ...state, activityTypes: action.data };
    case ActivityTypeActionTypes.SaveActivityTypeSuccess:
      return action.isNew ? insert(state, action) : update(state, action);
    case ActivityTypeActionTypes.RemoveActivityType:
      return remove(state, action);

    default:
      return state;
  }
}

/**
 * State selector helper.
 */
const featureSelector = createFeatureSelector<State>('hawk');
export const getActivityTypeState = createSelector(
  featureSelector,
  (state: State): ActivityTypeState => state.activityType,
);

export const getActivityTypes = createSelector(
  getActivityTypeState,
  (state: ActivityTypeState) => state.activityTypes,
);

/**
 * Remove an activity type from the activityTypes list
 */
function remove(
  state: ActivityTypeState,
  action: RemoveActivityType,
): ActivityTypeState {
  return {
    ...state,
    activityTypes: state.activityTypes.filter(
      (a: RavenActivityType) => a.id !== action.id,
    ),
  };
}

/**
 * Update an activity type in the activityTypes list
 */
function update(
  state: ActivityTypeState,
  action: SaveActivityTypeSuccess,
): ActivityTypeState {
  return {
    ...state,
    activityTypes: state.activityTypes.map((a: RavenActivityType) => {
      return a.id === action.data.id ? action.data : a;
    }),
  };
}

/**
 * Insert an activity type into the activity types list
 */
function insert(
  state: ActivityTypeState,
  action: SaveActivityTypeSuccess,
): ActivityTypeState {
  return {
    ...state,
    activityTypes: [...state.activityTypes, action.data],
  };
}
