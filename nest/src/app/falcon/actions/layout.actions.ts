/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';

export const loadingBarHide = createAction('[falcon-layout] loading_bar_hide');
export const loadingBarShow = createAction('[falcon-layout] loading_bar_show');

export const setPanelSizes = createAction(
  '[falcon-layout] set_panel_sizes',
  props<{ sizes: number[] }>(),
);

export const toggleEditorPanelsDirection = createAction(
  '[falcon-layout] toggle_editor_panels_direction',
);

export const toggleLeftPanelVisible = createAction(
  '[falcon-layout] toggle_left_panel_visible',
);

export const toggleRightPanelVisible = createAction(
  '[falcon-layout] toggle_right_panel_visible',
);
