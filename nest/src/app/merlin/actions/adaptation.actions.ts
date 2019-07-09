/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import { ActivityType, Adaptation } from '../../shared/models';

export const fetchActivityTypesFailure = createAction(
  '[adaptation] fetch_activity_types_failure',
  props<{ error: Error }>(),
);

export const fetchAdaptationsFailure = createAction(
  '[adaptation] fetch_adaptations_failure',
  props<{ error: Error }>(),
);

export const setActivityTypes = createAction(
  '[adaptation] set_activity_types',
  props<{ activityTypes: ActivityType[] }>(),
);

export const setAdaptations = createAction(
  '[adaptation] set_adaptations',
  props<{ adaptations: Adaptation[] }>(),
);
