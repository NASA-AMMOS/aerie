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
  RavenCustomFilterSource,
  RavenSource,
} from './../shared/models';

// Action Types.
export enum DialogActionTypes {
  OpenConfirmDialog       = '[dialog] open_confirm_dialog',
  OpenCustomFilterDialog  = '[dialog] open_custom_filter_dialog',
  OpenCustomGraphDialog   = '[dialog] open_custom_graph_dialog',
  OpenDeleteDialog        = '[dialog] open_delete_dialog',
  OpenFileImportDialog    = '[dialog] open_file_import_dialog',
  OpenLayoutApplyDialog   = '[dialog] open_layout_apply_dialog',
  OpenPinDialog           = '[dialog] open_pin_dialog',
  OpenShareableLinkDialog = '[dialog] open_shareable_link_dialog',
  OpenStateApplyDialog    = '[dialog] open_state_apply_dialog',
  OpenStateSaveDialog     = '[dialog] open_state_save_dialog',
}

// Actions.
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

export class OpenDeleteDialog implements Action {
  readonly type = DialogActionTypes.OpenDeleteDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenFileImportDialog implements Action {
  readonly type = DialogActionTypes.OpenFileImportDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenLayoutApplyDialog implements Action {
  readonly type = DialogActionTypes.OpenLayoutApplyDialog;

  constructor(public source: RavenSource, public width: string) {}
}

export class OpenPinDialog implements Action {
  readonly type = DialogActionTypes.OpenPinDialog;

  constructor(
    public pinAction: string,
    public source: RavenSource,
    public width: string,
  ) {}
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

// Union type of all actions.
export type DialogAction =
  OpenConfirmDialog |
  OpenCustomFilterDialog |
  OpenCustomGraphDialog |
  OpenDeleteDialog |
  OpenFileImportDialog |
  OpenLayoutApplyDialog |
  OpenPinDialog |
  OpenShareableLinkDialog |
  OpenStateApplyDialog |
  OpenStateSaveDialog;
