/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { OverlayModule } from '@angular/cdk/overlay';
import { TestBed } from '@angular/core/testing';
import { MatDialogModule } from '@angular/material';
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { StoreModule } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { DialogEffects } from './dialog.effects';

describe('DialogEffects', () => {
  let effects: DialogEffects;
  let metadata: EffectsMetadata<DialogEffects>;
  const actions: Observable<any> = of();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [MatDialogModule, OverlayModule, StoreModule.forRoot({})],
      providers: [DialogEffects, provideMockActions(() => actions)],
    });

    effects = TestBed.get(DialogEffects);
    metadata = getEffectsMetadata(effects);
  });

  it('should register openConfirmDialog$ that does not dispatch an action', () => {
    expect(metadata.openConfirmDialog$).toEqual({ dispatch: false });
  });

  it('should register openCustomFilterDialog$ that does dispatch an action', () => {
    expect(metadata.openCustomFilterDialog$).toEqual({ dispatch: true });
  });

  it('should register openCustomGraphDialog$ that does dispatch an action', () => {
    expect(metadata.openCustomGraphDialog$).toEqual({ dispatch: true });
  });

  it('should register openDeleteDialog$ that does dispatch an action', () => {
    expect(metadata.openDeleteDialog$).toEqual({ dispatch: true });
  });

  it('should register openDeleteBandDialog$ that does dispatch an action', () => {
    expect(metadata.openDeleteBandDialog$).toEqual({ dispatch: true });
  });

  it('should register openFileImportDialog$ that does dispatch an action', () => {
    expect(metadata.openFileImportDialog$).toEqual({ dispatch: true });
  });

  it('should register openFolderDialog$that does dispatch an action', () => {
    expect(metadata.openFolderDialog$).toEqual({ dispatch: true });
  });

  it('should register openPinDialog$that does dispatch an action', () => {
    expect(metadata.openPinDialog$).toEqual({ dispatch: true });
  });

  it('should register openRemoveAllGuidesDialog$ that does dispatch an action', () => {
    expect(metadata.openRemoveAllGuidesDialog$).toEqual({ dispatch: true });
  });

  it('should register openShareableLinkDialog$ that does not dispatch an action', () => {
    expect(metadata.openShareableLinkDialog$).toEqual({ dispatch: false });
  });

  it('should register openStateApplyDialog$ that does dispatch an action', () => {
    expect(metadata.openStateApplyDialog$).toEqual({ dispatch: true });
  });

  it('should register openStateSaveDialog$ that does dispatch an action', () => {
    expect(metadata.openStateSaveDialog$).toEqual({ dispatch: true });
  });
});
