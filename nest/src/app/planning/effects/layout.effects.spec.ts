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
import { cold, hot } from 'jasmine-marbles';
import { Observable } from 'rxjs';
import {
  Resize,
  ToggleActivityTypesDrawer,
  ToggleEditActivityDrawer,
} from '../actions/layout.actions';
import { LayoutEffects } from './layout.effects';

describe('LayoutEffects', () => {
  let actions$: Observable<any>;
  let effects: LayoutEffects;
  let metadata: EffectsMetadata<LayoutEffects>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [LayoutEffects, provideMockActions(() => actions$)],
    });

    effects = TestBed.get(LayoutEffects);
    metadata = getEffectsMetadata(effects);
  });

  describe('resize$', () => {
    it('should register resize$ that does not dispatch an action', () => {
      expect(metadata.resize$).toEqual({ dispatch: false });
    });
  });

  describe('toggleDrawer$', () => {
    it('should register toggleDrawer$ that dispatches an action', () => {
      expect(metadata.toggleDrawer$).toEqual({ dispatch: true });
    });

    it('should return a Resize() for a ToggleActivityTypesDrawer', () => {
      const action = new ToggleActivityTypesDrawer();
      const result = new Resize();

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: result });

      expect(effects.toggleDrawer$).toBeObservable(expected);
    });

    it('should return a Resize() for a ToggleEditActivityDrawer', () => {
      const action = new ToggleEditActivityDrawer();
      const result = new Resize();

      actions$ = hot('-a', { a: action });
      const expected = cold('-b', { b: result });

      expect(effects.toggleDrawer$).toBeObservable(expected);
    });
  });
});
