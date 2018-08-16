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

import { EpochsEffects } from './epochs.effect';

import { HttpClient, HttpClientModule } from '@angular/common/http';
import { MaterialModule } from './../../shared/material';

describe('EpochsEffects', () => {
  let effects: EpochsEffects;
  let metadata: EffectsMetadata<EpochsEffects>;
  const actions: Observable<any> = of();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientModule,
        MaterialModule,
        StoreModule.forRoot({}),
      ],
      providers: [
        HttpClient,
        EpochsEffects,
        provideMockActions(() => actions),
      ],
    });

    effects = TestBed.get(EpochsEffects);
    metadata = getEffectsMetadata(effects);
  });

  it('should register fetchEpochs$ that does dispatch an action', () => {
    expect(metadata.fetchEpochs$).toEqual({ dispatch: true });
  });
});
