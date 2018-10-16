/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Action } from '@ngrx/store';
import { BaseType, StringTMap } from '../models';

export enum NavigationDrawerStates {
  Opened = 'opened',
  Closed = 'closed',
  Collapsed = 'collapsed',
}

// Action Types.
export enum ConfigActionTypes {
  FetchProjectConfig = '[config] fetch_project_config',
  ToggleNavigationDrawer = '[config] toggle_menu',
  UpdateDefaultBandSettings = '[config] update_default_band_settings',
  UpdateRavenSettings = '[config] update_raven_settings',
}

// Actions.
export class FetchProjectConfig implements Action {
  readonly type = ConfigActionTypes.FetchProjectConfig;

  constructor(public url: string) {}
}

export class ToggleNavigationDrawer implements Action {
  readonly type = ConfigActionTypes.ToggleNavigationDrawer;
}

export class UpdateDefaultBandSettings implements Action {
  readonly type = ConfigActionTypes.UpdateDefaultBandSettings;

  constructor(public update: StringTMap<BaseType>) {}
}

export class UpdateRavenSettings implements Action {
  readonly type = ConfigActionTypes.UpdateRavenSettings;

  constructor(public update: StringTMap<BaseType>) {}
}

// Union type of all actions.
export type ConfigAction =
  | FetchProjectConfig
  | ToggleNavigationDrawer
  | UpdateDefaultBandSettings
  | UpdateRavenSettings;
