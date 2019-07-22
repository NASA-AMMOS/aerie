/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import { BaseType, StringTMap } from '../../shared/models';
import { RavenEpoch } from '../models';

export const addEpochs = createAction(
  '[raven-epochs] add_epochs',
  props<{ epochs: RavenEpoch[] }>(),
);

export const appendAndReplaceEpochs = createAction(
  '[raven-epochs] append_and_replace_epochs',
  props<{ epochs: RavenEpoch[] }>(),
);

export const fetchEpochs = createAction(
  '[raven-epochs] fetch_epochs',
  props<{ url: string; replaceAction: string }>(),
);

export const removeEpochs = createAction(
  '[raven-epochs] remove_epochs',
  props<{ epochs: RavenEpoch[] }>(),
);

export const saveNewEpochFile = createAction(
  '[raven-epochs] save_new_epoch_file',
  props<{ filePathName: string }>(),
);

export const saveNewEpochFileSuccess = createAction(
  '[raven-epochs] save_new_epoch_file_success',
);

export const setInUseEpochByName = createAction(
  '[raven-epochs] set_in_use_epoch_by_name',
  props<{ epochName: string }>(),
);

export const updateEpochData = createAction(
  '[raven-epochs] update_epoch_data',
  props<{ index: number; data: RavenEpoch }>(),
);

export const updateEpochSetting = createAction(
  '[raven-epochs] update_epoch_setting',
  props<{ update: StringTMap<BaseType> }>(),
);

export const updateProjectEpochs = createAction(
  '[raven-epochs] update_project_epochs',
);

export const updateProjectEpochsSuccess = createAction(
  '[raven-epochs] update_project_epochs_success',
);
