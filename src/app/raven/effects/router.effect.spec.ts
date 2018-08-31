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
import { RouterEffects } from './router.effect';

describe('RouterEffects', () => {
  let effects: RouterEffects;
  let metadata: EffectsMetadata<RouterEffects>;
  const actions: Observable<any> = of();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [StoreModule.forRoot({})],
      providers: [RouterEffects, provideMockActions(() => actions)],
    });

    effects = TestBed.get(RouterEffects);
    metadata = getEffectsMetadata(effects);
  });

  it('should register routerNavigation$ that does dispatch an action', () => {
    expect(metadata.routerNavigation$).toEqual({ dispatch: true });
  });

  it('should not register loadLayout', () => {
    expect(metadata.loadLayout).toBeUndefined();
  });

  it('should not register loadShareableLink', () => {
    expect(metadata.loadShareableLink).toBeUndefined();
  });

  it('should not register loadState', () => {
    expect(metadata.loadState).toBeUndefined();
  });
});
