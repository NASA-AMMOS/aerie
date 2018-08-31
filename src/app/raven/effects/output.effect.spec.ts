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
import { OutputEffects } from './output.effect';

describe('OutputEffects', () => {
  let effects: OutputEffects;
  let metadata: EffectsMetadata<OutputEffects>;
  const actions: Observable<any> = of();

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule, StoreModule.forRoot({})],
      providers: [HttpClient, OutputEffects, provideMockActions(() => actions)],
    });

    effects = TestBed.get(OutputEffects);
    metadata = getEffectsMetadata(effects);
  });

  it('should register createOutput$ that does dispatch an action', () => {
    expect(metadata.createOutput$).toEqual({ dispatch: true });
  });

  it('should register writeFile$ that does not dispatch an action', () => {
    expect(metadata.writeFile$).toEqual({ dispatch: false });
  });

  it('should register writeFile$ that does not dispatch an action', () => {
    expect(metadata.writeFile$).toEqual({ dispatch: false });
  });

  it('should not register createFileForSource', () => {
    expect(metadata.createFileForSource).toBeUndefined();
  });

  it('should not register generateOutputFile', () => {
    expect(metadata.generateOutputFile).toBeUndefined();
  });

  it('should not register generateOutputFiles', () => {
    expect(metadata.generateOutputFiles).toBeUndefined();
  });

  it('should not register getCsvDataForSource', () => {
    expect(metadata.getCsvDataForSource).toBeUndefined();
  });

  it('should not register removeCsvHeader', () => {
    expect(metadata.removeCsvHeader).toBeUndefined();
  });

  it('should not register sanitizeData', () => {
    expect(metadata.sanitizeData).toBeUndefined();
  });

  it('should not register writeToFile', () => {
    expect(metadata.writeToFile).toBeUndefined();
  });
});
