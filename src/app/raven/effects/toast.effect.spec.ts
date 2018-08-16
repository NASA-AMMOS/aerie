/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { TestBed } from '@angular/core/testing';
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { StoreModule } from '@ngrx/store';
import { Observable, of } from 'rxjs';

import { ToastrModule, ToastrService } from 'ngx-toastr';
import { ToastEffects } from './toast.effect';

import { MaterialModule } from './../../shared/material';

describe('ToastEffects', () => {
  let effects: ToastEffects;
  let metadata: EffectsMetadata<ToastEffects>;
  const actions: Observable<any> = of();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        MaterialModule,
        StoreModule.forRoot({}),
        ToastrModule.forRoot(),
      ],
      providers: [
        ToastEffects,
        ToastrService,
        provideMockActions(() => actions),
      ],
    });

    effects = TestBed.get(ToastEffects);
    metadata = getEffectsMetadata(effects);
  });

  it('should register showToast$ that does not dispatch an action', () => {
    expect(metadata.showToast$).toEqual({ dispatch: false });
  });
});
