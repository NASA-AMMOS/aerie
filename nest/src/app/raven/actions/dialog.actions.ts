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
  RavenCompositeBand,
  RavenCustomFilterSource,
  RavenSource,
} from '../../shared/models';

// Action Types.
export enum DialogActionTypes {
  OpenApplyCurrentStateDialog = '[dialog] open_apply_current_state_dialog',
  OpenConfirmDialog = '[dialog] open_confirm_dialog',
  OpenCustomFilterDialog = '[dialog] open_custom_filter_dialog',
  OpenCustomGraphDialog = '[dialog] open_custom_graph_dialog',
  OpenDeleteBandDialog = '[dialog] open_delete_band_dialog',
  OpenDeleteDialog = '[dialog] open_delete_dialog',
  OpenFileImportDialog = '[dialog] open_file_import_dialog',
  OpenFolderDialog = '[dialog] open_folder_dialog',
  OpenPinDialog = '[dialog] open_pin_dialog',
  OpenRemoveAllBandsDialog = '[dialog] remove_all_bands_dialog',
  OpenRemoveAllGuidesDialog = '[dialog] remove_all_guides_dialog',
  OpenSettingsBandDialog = '[dialog] open_settings_band_dialog',
  OpenShareableLinkDialog = '[dialog] open_shareable_link_dialog',
  OpenStateApplyDialog = '[dialog] open_state_apply_dialog',
  OpenStateSaveDialog = '[dialog] open_state_save_dialog',
  OpenUpdateCurrentStateDialog = '[dialog open_update_current_state_dialog',
}

// Actions.
export class OpenApplyCurrentStateDialog implements Action {
  readonly type = DialogActionTypes.OpenApplyCurrentStateDialog;
}

export class OpenConfirmDialog implements Action {
  readonly type = DialogActionTypes.OpenConfirmDialog;

  constructor(
    public cancelText: string,
    public message: string,
    public width: string,
  ) {}
}

export class OpenCustomFilterDialog implements Action {
  readonly type = DialogActionTypes.OpenCustomFilterDialog;

  constructor(public source: RavenCustomFilterSource, public width: string) {}
}

export class OpenCustomGraphDialog implements Action {
  readonly type = DialogActionTypes.OpenCustomGraphDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenDeleteBandDialog implements Action {
  readonly type = DialogActionTypes.OpenDeleteBandDialog;

  constructor(public band: RavenCompositeBand, public width: string) {}
}

export class OpenDeleteDialog implements Action {
  readonly type = DialogActionTypes.OpenDeleteDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenFileImportDialog implements Action {
  readonly type = DialogActionTypes.OpenFileImportDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenFolderDialog implements Action {
  readonly type = DialogActionTypes.OpenFolderDialog;

  constructor(
    public folderAction: string,
    public source: RavenSource,
    public width: string,
  ) {}
}

export class OpenPinDialog implements Action {
  readonly type = DialogActionTypes.OpenPinDialog;

  constructor(
    public pinAction: string,
    public source: RavenSource,
    public width: string,
  ) {}
}

export class OpenRemoveAllBandsDialog implements Action {
  readonly type = DialogActionTypes.OpenRemoveAllBandsDialog;

  constructor(public width: string) {}
}

export class OpenRemoveAllGuidesDialog implements Action {
  readonly type = DialogActionTypes.OpenRemoveAllGuidesDialog;

  constructor(public width: string) {}
}

export class OpenSettingsBandDialog implements Action {
  readonly type = DialogActionTypes.OpenSettingsBandDialog;

  constructor(public bandId: string, public subBandId: string, public width: string) {}
}

export class OpenShareableLinkDialog implements Action {
  readonly type = DialogActionTypes.OpenShareableLinkDialog;

  constructor(public width: string) {}
}

export class OpenStateApplyDialog implements Action {
  readonly type = DialogActionTypes.OpenStateApplyDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenStateSaveDialog implements Action {
  readonly type = DialogActionTypes.OpenStateSaveDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenUpdateCurrentStateDialog implements Action {
  readonly type = DialogActionTypes.OpenUpdateCurrentStateDialog;
}

// Union type of all actions.
export type DialogAction =
  | OpenApplyCurrentStateDialog
  | OpenConfirmDialog
  | OpenCustomFilterDialog
  | OpenCustomGraphDialog
  | OpenDeleteBandDialog
  | OpenDeleteDialog
  | OpenFileImportDialog
  | OpenFolderDialog
  | OpenPinDialog
  | OpenRemoveAllBandsDialog
  | OpenRemoveAllGuidesDialog
  | OpenSettingsBandDialog
  | OpenShareableLinkDialog
  | OpenStateApplyDialog
  | OpenStateSaveDialog
  | OpenUpdateCurrentStateDialog;
