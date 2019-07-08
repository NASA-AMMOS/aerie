/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { addMatchers, cold, hot, initTestScheduler } from 'jasmine-marbles';
import { Observable } from 'rxjs';
import { LayoutActions } from '../actions';
import { LayoutEffects } from './layout.effects';

describe('LayoutEffects', () => {
  let actions: Observable<any>;
  let effects: LayoutEffects;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LayoutEffects, provideMockActions(() => actions)],
    });

    initTestScheduler();
    addMatchers();
    effects = TestBed.get(LayoutEffects);
  });

  describe('toggleDrawer$', () => {
    it('should return a Resize() for a ToggleActivityTypesDrawer', () => {
      const action = LayoutActions.toggleActivityTypesDrawer({});
      const result = LayoutActions.resize({});

      actions = hot('-a', { a: action });
      const expected = cold('-b', { b: result });

      expect(effects.toggleDrawer).toBeObservable(expected);
    });

    it('should return a Resize() for a ToggleEditActivityDrawer', () => {
      const action = LayoutActions.toggleEditActivityDrawer({});
      const result = LayoutActions.resize({});

      actions = hot('-a', { a: action });
      const expected = cold('-b', { b: result });

      expect(effects.toggleDrawer).toBeObservable(expected);
    });
  });
});
