/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';

export const closeAllDrawers = createAction('[layout] close_all_drawers');
export const loadingBarHide = createAction('[layout] loading_bar_hide');
export const loadingBarShow = createAction('[layout] loading_bar_show');

export const resize = createAction(
  '[layout] resize',
  props<{ timeout?: number }>(),
);

export const toggleActivityTypesDrawer = createAction(
  '[layout] toggle_activity_types_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleAddActivityDrawer = createAction(
  '[layout] toggle_add_activity_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleCreatePlanDrawer = createAction(
  '[layout] toggle_create_plan_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleEditActivityDrawer = createAction(
  '[layout] toggle_edit_activity_drawer',
  props<{ opened?: boolean }>(),
);
