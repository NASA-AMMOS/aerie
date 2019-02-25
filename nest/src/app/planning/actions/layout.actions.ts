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
  LoadingBarHide = '[layout] loading_bar_hide',
  LoadingBarShow = '[layout] loading_bar_show',
  Resize = '[layout] resize',
  ToggleActivityTypesDrawer = '[layout] toggle_activity_types_drawer',
  ToggleEditActivityDrawer = '[layout] toggle_edit_activity_drawer',
}

export class LoadingBarHide implements Action {
  readonly type = LayoutActionTypes.LoadingBarHide;
}

export class LoadingBarShow implements Action {
  readonly type = LayoutActionTypes.LoadingBarShow;
}

export class Resize implements Action {
  readonly type = LayoutActionTypes.Resize;
  constructor(public timeout?: number) {}
}

export class ToggleActivityTypesDrawer implements Action {
  readonly type = LayoutActionTypes.ToggleActivityTypesDrawer;
  constructor(public opened?: boolean) {}
}

export class ToggleEditActivityDrawer implements Action {
  readonly type = LayoutActionTypes.ToggleEditActivityDrawer;
  constructor(public opened?: boolean) {}
}

export type LayoutActions =
  | LoadingBarHide
  | LoadingBarShow
  | Resize
  | ToggleActivityTypesDrawer
  | ToggleEditActivityDrawer;
