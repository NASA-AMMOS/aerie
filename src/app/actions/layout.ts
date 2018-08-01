/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

import {
  BaseType,
  StringTMap,
} from './../shared/models';

// Action Types.
export enum LayoutActionTypes {
  Resize                     = '[layout] resize',
  SetMode                    = '[layout] set_mode',
  ToggleApplyLayoutDrawer    = '[layout] toggle_apply_layout_drawer',
  ToggleDetailsPanel         = '[layout] toggle_details_panel',
  ToggleEpochsDrawer         = '[layout] toggle_epochs_drawer',
  ToggleGlobalSettingsDrawer = '[layout] toggle_global_settings_drawer',
  ToggleLeftPanel            = '[layout] toggle_left_panel',
  ToggleOutputDrawer         = '[layout] toggle_output_drawer',
  ToggleRightPanel           = '[layout] toggle_right_panel',
  ToggleSouthBandsPanel      = '[layout] toggle_south_bands_panel',
  ToggleTimeCursorDrawer     = '[layout] toggle_time_cursor_drawer',
  UpdateLayout               = '[layout] update_layout',
}

// Actions.
export class Resize implements Action {
  readonly type = LayoutActionTypes.Resize;
}

export class SetMode implements Action {
  readonly type = LayoutActionTypes.SetMode;

  constructor(
    public mode: string,
    public showDetailsPanel: boolean,
    public showLeftPanel: boolean,
    public showRightPanel: boolean,
    public showSouthBandsPanel: boolean,
  ) {}
}

export class ToggleApplyLayoutDrawer implements Action {
  readonly type = LayoutActionTypes.ToggleApplyLayoutDrawer;

  constructor(public opened?: boolean) {}
}

export class ToggleDetailsPanel implements Action {
  readonly type = LayoutActionTypes.ToggleDetailsPanel;
}

export class ToggleEpochsDrawer implements Action {
  readonly type = LayoutActionTypes.ToggleEpochsDrawer;

  constructor(public opened?: boolean) {}
}

export class ToggleGlobalSettingsDrawer implements Action {
  readonly type = LayoutActionTypes.ToggleGlobalSettingsDrawer;

  constructor(public opened?: boolean) {}
}

export class ToggleLeftPanel implements Action {
  readonly type = LayoutActionTypes.ToggleLeftPanel;
}

export class ToggleOutputDrawer implements Action {
  readonly type = LayoutActionTypes.ToggleOutputDrawer;

  constructor(public opened?: boolean) {}
}

export class ToggleRightPanel implements Action {
  readonly type = LayoutActionTypes.ToggleRightPanel;
}

export class ToggleSouthBandsPanel implements Action {
  readonly type = LayoutActionTypes.ToggleSouthBandsPanel;
}

export class ToggleTimeCursorDrawer implements Action {
  readonly type = LayoutActionTypes.ToggleTimeCursorDrawer;

  constructor(public opened?: boolean) {}
}

export class UpdateLayout implements Action {
  readonly type = LayoutActionTypes.UpdateLayout;

  constructor(public update: StringTMap<BaseType>) {}
}

// Union type of all actions.
export type LayoutAction =
  Resize |
  SetMode |
  ToggleApplyLayoutDrawer |
  ToggleDetailsPanel |
  ToggleEpochsDrawer |
  ToggleGlobalSettingsDrawer |
  ToggleLeftPanel |
  ToggleOutputDrawer |
  ToggleRightPanel |
  ToggleSouthBandsPanel |
  ToggleTimeCursorDrawer |
  UpdateLayout;
