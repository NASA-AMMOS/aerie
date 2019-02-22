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
import { TimeCursorEffects } from './time-cursor.effects';

import * as timeCursorReducer from '../reducers/time-cursor.reducer';

describe('TimeCursorEffects', () => {
  let effects: TimeCursorEffects;
  let metadata: EffectsMetadata<TimeCursorEffects>;
  const actions: Observable<any> = of();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [StoreModule.forRoot(timeCursorReducer.reducer)],
      providers: [TimeCursorEffects, provideMockActions(() => actions)],
    });

    effects = TestBed.get(TimeCursorEffects);
    metadata = getEffectsMetadata(effects);
  });

  it('should register hideTimeCursor$ that does not dispatch an action', () => {
    expect(metadata.hideTimeCursor$).toEqual({ dispatch: false });
  });

  it('should register showTimeCursor$ that dispatches an action', () => {
    expect(metadata.showTimeCursor$).toEqual({ dispatch: true });
  });

  it('should not register cursorInterval$', () => {
    expect(metadata.cursorInterval$).toBeUndefined();
  });

  it('should not register updateCursor', () => {
    expect(metadata.updateCursor).toBeUndefined();
  });
});
