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

export enum NavigationDrawerStates {
  Opened = 'opened',
  Closed = 'closed',
  Collapsed = 'collapsed',
}

export const fetchProjectConfig = createAction(
  '[nest-config] fetch_project_config',
  props<{ url: string }>(),
);

export const fetchProjectConfigSuccess = createAction(
  '[nest-config] fetch_project_config_success',
);

export const toggleNestNavigationDrawer = createAction(
  '[nest-config] toggle_nest_navigation_drawer',
);

export const updateDefaultBandSettings = createAction(
  '[nest-config] update_default_band_settings',
  props<{ update: StringTMap<BaseType> }>(),
);

export const updateMpsServerSettings = createAction(
  '[nest-config] update_mpsserver_settings',
  props<{ update: StringTMap<BaseType> }>(),
);

export const updateRavenSettings = createAction(
  '[nest-config] update_raven_settings',
  props<{ update: StringTMap<BaseType> }>(),
);
