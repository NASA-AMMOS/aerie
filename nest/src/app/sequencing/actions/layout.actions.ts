/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';

export enum LayoutActionTypes {
  LoadingBarHide = '[sequencing-layout] loading_bar_hide',
  LoadingBarShow = '[sequencing-layout] loading_bar_show',
  SetPanelSizes = '[sequencing-layout] set_panel_sizes',
  ToggleLeftPanelVisible = '[sequencing-layout] toggle_left_panel_visible',
  ToggleRightPanelVisible = '[sequencing-layout] toggle_right_panel_visible',
}

export class LoadingBarHide implements Action {
  readonly type = LayoutActionTypes.LoadingBarHide;
}

export class LoadingBarShow implements Action {
  readonly type = LayoutActionTypes.LoadingBarShow;
}

export class SetPanelSizes implements Action {
  readonly type = LayoutActionTypes.SetPanelSizes;
  constructor(public sizes: number[]) {}
}

export class ToggleLeftPanelVisible implements Action {
  readonly type = LayoutActionTypes.ToggleLeftPanelVisible;
}

export class ToggleRightPanelVisible implements Action {
  readonly type = LayoutActionTypes.ToggleRightPanelVisible;
}

export type LayoutActions =
  | LoadingBarHide
  | LoadingBarShow
  | SetPanelSizes
  | ToggleLeftPanelVisible
  | ToggleRightPanelVisible;
