/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient, HttpClientModule } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { StoreModule } from '@ngrx/store';
import { addMatchers, cold, hot, initTestScheduler } from 'jasmine-marbles';
import { Observable } from 'rxjs';
import { ROOT_REDUCERS } from '../../app-store';
import { SourceExplorerActions } from '../actions';
import { FilterState } from '../models';
import {
  MockedFilters,
  MpsServerMockService,
} from '../services/mps-server-mock.service';
import { MpsServerService } from '../services/mps-server.service';
import { SourceExplorerEffects } from './source-explorer.effects';

describe('SourceExplorerEffects', () => {
  let effects: SourceExplorerEffects;
  let actions: Observable<any>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule, StoreModule.forRoot(ROOT_REDUCERS)],
      providers: [
        HttpClient,
        SourceExplorerEffects,
        {
          provide: MpsServerService,
          useValue: new MpsServerMockService(),
        },
        provideMockActions(() => actions),
      ],
    });

    initTestScheduler();
    addMatchers();
    effects = TestBed.get(SourceExplorerEffects);
  });

  it('should emit UpdateSourceExplorer when an empty filter is applied', () => {
    const filterState: FilterState = FilterState.empty();

    actions = hot('a', {
      a: SourceExplorerActions.updateSourceFilter({
        sourceFilter: filterState.filter,
      }),
    });

    const expected = cold('(bc)', {
      b: SourceExplorerActions.updateSourceExplorer({
        update: { fetchPending: true },
      }),
      c: SourceExplorerActions.updateSourceExplorer({
        update: { fetchPending: false, filterState },
      }),
    });

    expect(effects.updateSourceFilter).toBeObservable(expected);
  });

  it('should emit UpdateSourceExplorer when a filter is applied successfully', () => {
    const filterState: FilterState = {
      filter: MockedFilters['name matches abc'],
      filteredSources: new Set(['/mongo/db1/abc', '/mongo/db1/xabcy']),
      visibleAncestors: new Set([
        '/mongo',
        '/mongo/db1',
        '/mongo/db1/abc',
        '/mongo/db1/xabcy',
      ]),
    };

    actions = hot('a', {
      a: SourceExplorerActions.updateSourceFilter({
        sourceFilter: filterState.filter,
      }),
    });

    const expected = cold('(bc)', {
      b: SourceExplorerActions.updateSourceExplorer({
        update: { fetchPending: true },
      }),
      c: SourceExplorerActions.updateSourceExplorer({
        update: { fetchPending: false, filterState },
      }),
    });

    expect(effects.updateSourceFilter).toBeObservable(expected);
  });
});
