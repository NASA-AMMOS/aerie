/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { createAction, props } from '@ngrx/store';
import { SequenceFile } from '../../../../../sequencing/src/models';

export const addEditor = createAction('[falcon-file] add_editor');

export const createTab = createAction(
  '[falcon-file] create_tab',
  props<{ editorId: string; id?: string }>(),
);

export const closeTab = createAction(
  '[falcon-file] close_tab',
  props<{ docIdToClose: string; editorId: string }>(),
);

export const fetchChildren = createAction(
  '[falcon-file] fetch_children',
  props<{ parentId: string; options?: any }>(),
);

export const fetchChildrenFailure = createAction(
  '[falcon-file] fetch_children_failure',
  props<{ error: Error }>(),
);

export const setActiveEditor = createAction(
  '[falcon-file] set_active_editor',
  props<{ editorId: string }>(),
);

export const switchTab = createAction(
  '[falcon-file] switch_tab',
  props<{ editorId: string; switchToId: string }>(),
);

export const updateChildren = createAction(
  '[falcon-file] update_children',
  props<{ parentId: string; children: SequenceFile[]; options?: any }>(),
);

export const updateTab = createAction(
  '[falcon-file] update_tab',
  props<{ docIdToUpdate: string; editorId: string; text: string }>(),
);
