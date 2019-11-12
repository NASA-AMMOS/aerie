/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import {
  RavenCompositeBand,
  RavenCustomFilterSource,
  RavenSource,
} from '../models';

export const openAboutDialog = createAction(
  '[raven-dialog] open_about_dialog',
  props<{ width: string }>(),
);

export const openApplyCurrentStateDialog = createAction(
  '[raven-dialog] open_apply_current_state_dialog',
);

export const openConfirmDialog = createAction(
  '[raven-dialog] open_confirm_dialog',
  props<{ cancelText: string; message: string; width: string }>(),
);

export const openCustomFilterDialog = createAction(
  '[raven-dialog] open_custom_filter_dialog',
  props<{ source: RavenCustomFilterSource; width: string }>(),
);

export const openCustomGraphDialog = createAction(
  '[raven-dialog] open_custom_graph_dialog',
  props<{ source: RavenSource; width: string }>(),
);

export const openDeleteBandDialog = createAction(
  '[raven-dialog] open_delete_band_dialog',
  props<{ band: RavenCompositeBand; width: string }>(),
);

export const openDeleteSourceDialog = createAction(
  '[raven-dialog] open_delete_source_dialog',
  props<{ source: RavenSource; width: string }>(),
);

export const openFileImportDialog = createAction(
  '[raven-dialog] open_file_import_dialog',
  props<{ source: RavenSource; width: string }>(),
);

export const openFolderDialog = createAction(
  '[raven-dialog] open_folder_dialog',
  props<{ folderAction: string; source: RavenSource; width: string }>(),
);

export const openLoadEpochDialog = createAction(
  '[raven-dialog] open_load_epoch_dialog',
  props<{ sourceUrl: string }>(),
);

export const openPinDialog = createAction(
  '[raven-dialog] open_pin_dialog',
  props<{ pinAction: string; source: RavenSource; width: string }>(),
);

export const openRemoveAllBandsDialog = createAction(
  '[raven-dialog] open_remove_all_bands_dialog',
  props<{ width: string }>(),
);

export const openRemoveAllGuidesDialog = createAction(
  '[raven-dialog] open_remove_all_guides_dialog',
  props<{ width: string }>(),
);

export const openSaveNewEpochFileDialog = createAction(
  '[dialog open_save_new_epoch_file_dialog',
);

export const openSettingsBandDialog = createAction(
  '[raven-dialog] open_settings_band_dialog',
  props<{ bandId: string; subBandId: string; width: string }>(),
);

export const openShareableLinkDialog = createAction(
  '[raven-dialog] open_shareable_link_dialog',
  props<{ width: string }>(),
);

export const openStateApplyDialog = createAction(
  '[raven-dialog] open_state_apply_dialog',
  props<{ source: RavenSource; width: string }>(),
);

export const openStateSaveDialog = createAction(
  '[raven-dialog] open_state_save_dialog',
  props<{ source: RavenSource; width: string }>(),
);

export const openUpdateCurrentStateDialog = createAction(
  '[raven-dialog] open_update_current_state_dialog',
);

export const openUpdateProjectEpochsDialog = createAction(
  '[raven-dialog] open_update_project_epochs_dialog',
);
