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

export const fetchProjectConfig = createAction(
  '[raven-config] fetch_project_config',
  props<{ url: string }>(),
);

export const fetchProjectConfigSuccess = createAction(
  '[raven-config] fetch_project_config_success',
);

export const updateDefaultBandSettings = createAction(
  '[raven-config] update_default_band_settings',
  props<{ update: StringTMap<BaseType> }>(),
);

export const updateMpsServerSettings = createAction(
  '[raven-config] update_mpsserver_settings',
  props<{ update: StringTMap<BaseType> }>(),
);

export const updateRavenSettings = createAction(
  '[raven-config] update_raven_settings',
  props<{ update: StringTMap<BaseType> }>(),
);
