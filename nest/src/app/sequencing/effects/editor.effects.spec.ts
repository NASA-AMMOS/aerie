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
import { AddNewLineWithText } from '../actions/editor.actions';
import { SeqEditorService } from '../services/seq-editor.service';
import { EditorEffects } from './editor.effects';

describe('EditorEffects', () => {
  let actions$: Observable<any>;
  let effects: EditorEffects;
  let metadata: EffectsMetadata<EditorEffects>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        EditorEffects,
        provideMockActions(() => actions$),
        {
          provide: SeqEditorService,
          useValue: new SeqEditorService(),
        },
      ],
    });

    effects = TestBed.get(EditorEffects);
    metadata = getEffectsMetadata(effects);
  });

  describe('addNewLineWithText$', () => {
    it('should register addNewLineWithText$ that does not dispatch an action', () => {
      expect(metadata.addNewLineWithText$).toEqual({ dispatch: false });
    });

    it('should not dispatch an action for AddNewLineWithText but should call addNewLineWithText in the SeqEditorService', () => {
      const action = new AddNewLineWithText('that was easy');
      const service = TestBed.get(SeqEditorService);
      const addNewLineWithText = spyOn(service, 'addNewLineWithText');

      actions$ = hot('-a', { a: action });
      const expected = cold('-');

      expect(effects.addNewLineWithText$).toBeObservable(expected);
      expect(addNewLineWithText).toHaveBeenCalled();
    });
  });
});
