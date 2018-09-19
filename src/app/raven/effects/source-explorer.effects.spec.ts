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
import { EffectsMetadata, getEffectsMetadata } from '@ngrx/effects';
import { provideMockActions } from '@ngrx/effects/testing';
import { StoreModule } from '@ngrx/store';
import { Observable, of } from 'rxjs';
import { MpsServerService } from '../../shared/services/mps-server.service';
import { SourceExplorerEffects } from './source-explorer.effects';

describe('SourceExplorerEffects', () => {
  let effects: SourceExplorerEffects;
  let metadata: EffectsMetadata<SourceExplorerEffects>;
  let mpsServerService: any;
  const actions: Observable<any> = of();

  beforeEach(() => {
    mpsServerService = jasmine.createSpyObj('MpsServerService', [
      'fetchNewSources',
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientModule, StoreModule.forRoot({})],
      providers: [
        HttpClient,
        SourceExplorerEffects,
        {
          provide: MpsServerService,
          useValue: mpsServerService,
        },
        provideMockActions(() => actions),
      ],
    });

    effects = TestBed.get(SourceExplorerEffects);
    metadata = getEffectsMetadata(effects);
  });

  it('should register addCustomGraph$ that does dispatch an action', () => {
    expect(metadata.addCustomGraph$).toEqual({ dispatch: true });
  });

  it('should register addGraphableFilter$ that does dispatch an action', () => {
    expect(metadata.addGraphableFilter$).toEqual({ dispatch: true });
  });

  it('should register applyLayout$ that does dispatch an action', () => {
    expect(metadata.applyLayout$).toEqual({ dispatch: true });
  });

  it('should register applyState$ that does dispatch an action', () => {
    expect(metadata.applyState$).toEqual({ dispatch: true });
  });

  it('should register closeEvent$ that does dispatch an action', () => {
    expect(metadata.closeEvent$).toEqual({ dispatch: true });
  });

  it('should register expandEvent$ that does dispatch an action', () => {
    expect(metadata.expandEvent$).toEqual({ dispatch: true });
  });

  it('should register fetchInitialSources$ that does dispatch an action', () => {
    expect(metadata.fetchInitialSources$).toEqual({ dispatch: true });
  });

  it('should register fetchNewSources$ that does dispatch an action', () => {
    expect(metadata.fetchNewSources$).toEqual({ dispatch: true });
  });

  it('should register graphCustomSource$ that does dispatch an action', () => {
    expect(metadata.graphCustomSource$).toEqual({ dispatch: true });
  });

  it('should register importFile$ that does dispatch an action', () => {
    expect(metadata.importFile$).toEqual({ dispatch: true });
  });

  it('should register loadErrorsDisplay$ that does dispatch an action', () => {
    expect(metadata.loadErrorsDisplay$).toEqual({ dispatch: true });
  });

  it('should register openEvent$ that does dispatch an action', () => {
    expect(metadata.openEvent$).toEqual({ dispatch: true });
  });

  it('should register removeGraphableFilter$ that does dispatch an action', () => {
    expect(metadata.removeGraphableFilter$).toEqual({ dispatch: true });
  });

  it('should register removeSourceEvent$ that does dispatch an action', () => {
    expect(metadata.removeSourceEvent$).toEqual({ dispatch: true });
  });

  it('should register saveState$ that does dispatch an action', () => {
    expect(metadata.saveState$).toEqual({ dispatch: true });
  });

  it('should register updateGraphAfterFilterAdd$ that does dispatch an action', () => {
    expect(metadata.updateGraphAfterFilterAdd$).toEqual({ dispatch: true });
  });

  it('should register updateGraphAfterFilterRemove$ that does dispatch an action', () => {
    expect(metadata.updateGraphAfterFilterRemove$).toEqual({ dispatch: true });
  });

  it('should not register expand', () => {
    expect(metadata.expand).toBeUndefined();
  });

  it('should not register fetchSubBands', () => {
    expect(metadata.fetchSubBands).toBeUndefined();
  });

  it('should not register loadLayout', () => {
    expect(metadata.loadLayout).toBeUndefined();
  });

  it('should not register loadState', () => {
    expect(metadata.loadState).toBeUndefined();
  });

  it('should not register load', () => {
    expect(metadata.load).toBeUndefined();
  });

  it('should not register open', () => {
    expect(metadata.open).toBeUndefined();
  });

  it('should not register openAllInstancesForSource', () => {
    expect(metadata.openAllInstancesForSource).toBeUndefined();
  });

  it('should not register fetchNewSources', () => {
    expect(metadata.fetchNewSources).toBeUndefined();
  });

  it('should not register fetchSourcesByType', () => {
    expect(metadata.fetchSourcesByType).toBeUndefined();
  });

  it('should not register removeSource', () => {
    expect(metadata.removeSource).toBeUndefined();
  });

  it('should not register restoreFilters', () => {
    expect(metadata.restoreFilters).toBeUndefined();
  });

  it('should not register fetchState', () => {
    expect(metadata.fetchState).toBeUndefined();
  });

  it('should not register saveState', () => {
    expect(metadata.saveState).toBeUndefined();
  });

  it('should not register importMappingFile', () => {
    expect(metadata.importMappingFile).toBeUndefined();
  });
});
