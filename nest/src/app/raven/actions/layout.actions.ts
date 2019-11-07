/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import { BaseType, StringTMap } from '../models';

export const resize = createAction('[raven-layout] resize');

export const setMode = createAction(
  '[raven-layout] set_mode',
  props<{
    mode: string;
    showDetailsPanel: boolean;
    showLeftPanel: boolean;
    showRightPanel: boolean;
    showSouthBandsPanel: boolean;
  }>(),
);

export const toggleApplyLayoutDrawer = createAction(
  '[raven-layout] toggle_apply_layout_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleApplyLayoutDrawerEvent = createAction(
  '[raven-layout] toggle_apply_layout_drawer_event',
  props<{ opened?: boolean }>(),
);

export const toggleDetailsPanel = createAction(
  '[raven-layout] toggle_details_panel',
);

export const toggleEpochsDrawer = createAction(
  '[raven-layout] toggle_epochs_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleFileMetadataDrawer = createAction(
  '[raven-layout] toggle_file_metadata_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleGlobalSettingsDrawer = createAction(
  '[raven-layout] toggle_global_settings_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleLeftPanel = createAction('[raven-layout] toggle_left_panel');

export const toggleOutputDrawer = createAction(
  '[raven-layout] toggle_output_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleRightPanel = createAction(
  '[raven-layout] toggle_right_panel',
);

export const toggleSituationalAwarenessDrawer = createAction(
  '[raven-layout] toggle_situational_awareness_drawer',
  props<{ opened?: boolean }>(),
);

export const toggleSouthBandsPanel = createAction(
  '[raven-layout] toggle_south_bands_panel',
);

export const toggleTimeCursorDrawer = createAction(
  '[raven-layout] toggle_time_cursor_drawer',
  props<{ opened?: boolean }>(),
);

export const updateLayout = createAction(
  '[raven-layout] update_layout',
  props<{ update: StringTMap<BaseType> }>(),
);
