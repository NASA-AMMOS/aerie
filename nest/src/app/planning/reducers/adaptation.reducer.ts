/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ActivityType, Adaptation } from '../../../../libs/schemas/types/ts';
import { StringTMap } from '../../shared/models';
import {
  AdaptationActions,
  AdaptationActionTypes,
} from '../actions/adaptation.actions';

export interface AdaptationState {
  activityTypes: StringTMap<ActivityType>;
  adaptations: Adaptation[];
}

export const initialState: AdaptationState = {
  activityTypes: {},
  adaptations: [],
};

/**
 * Reducer.
 * If a case takes more than one line then it should be in it's own helper function.
 */
export function reducer(
  state: AdaptationState = initialState,
  action: AdaptationActions,
): AdaptationState {
  switch (action.type) {
    case AdaptationActionTypes.FetchActivityTypesSuccess:
      return { ...state, activityTypes: action.data };
    case AdaptationActionTypes.FetchAdaptationsSuccess:
      return { ...state, adaptations: action.data };
    default:
      return state;
  }
}
