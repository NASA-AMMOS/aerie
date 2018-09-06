/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { saveAs } from 'file-saver';
import { concat, Observable, of } from 'rxjs';
import { getCustomFilterForLabel, getOutputDataUrl } from '../../shared/util';
import { OutputActionTypes } from '../actions/output.actions';
import { RavenAppState } from '../raven-store';

import {
  concatMap,
  exhaustMap,
  map,
  switchMap,
  withLatestFrom,
} from 'rxjs/operators';

import {
  RavenCustomFilter,
  RavenSource,
  StringTMap,
} from '../../shared/models';

import * as outputActions from '../actions/output.actions';
import * as fromOutput from '../reducers/output.reducer';

@Injectable()
export class OutputEffects {
  /**
   * Effect for CreateOutput.
   */
  @Effect()
  createOutput$: Observable<Action> = this.actions$.pipe(
    ofType<outputActions.CreateOutput>(OutputActionTypes.CreateOutput),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven),
    exhaustMap(({ output, sourceExplorer }) =>
      concat(
        ...(output.allInOneFile
          ? this.generateOutputFile(
              output,
              sourceExplorer.treeBySourceId,
              sourceExplorer.customFiltersBySourceId,
              sourceExplorer.filtersByTarget,
            )
          : this.generateOutputFiles(
              output,
              sourceExplorer.treeBySourceId,
              sourceExplorer.customFiltersBySourceId,
              sourceExplorer.filtersByTarget,
            )),
      ),
    ),
  );

  /**
   * Effect for WriteFile.
   */
  @Effect({ dispatch: false })
  writeFile$: Observable<Action> = this.actions$.pipe(
    ofType<outputActions.WriteFile>(OutputActionTypes.WriteFile),
    withLatestFrom(this.store$),
    map(([, state]) => state.raven),
    concatMap(({ output }) => {
      this.writeToFile(
        output.outputData,
        output.allInOneFilename,
        output.outputFormat.toLowerCase(),
      );
      return [];
    }),
  );

  constructor(
    private actions$: Actions,
    private http: HttpClient,
    private store$: Store<RavenAppState>,
  ) {}

  /**
   * Helper. Get data for source and write to file.
   */
  createFileForSource(
    output: fromOutput.OutputState,
    treeBySourceId: StringTMap<RavenSource>,
    customFilter: RavenCustomFilter | null,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
    source: RavenSource,
    label: string,
  ) {
    const outputDataUrl = getOutputDataUrl(
      treeBySourceId,
      source,
      customFilter,
      filtersByTarget,
      output.outputFormat,
      output.decimateOutputData,
    );

    if (output.outputFormat === 'CSV') {
      return this.http.get(outputDataUrl, { responseType: 'text' }).pipe(
        switchMap(data => {
          this.writeToFile(data, label, 'csv');
          return [];
        }),
      );
    } else {
      return this.http.get(outputDataUrl).pipe(
        map(data => JSON.stringify(data)),
        switchMap(jsonData => {
          this.writeToFile(jsonData, label, 'json');
          return [];
        }),
      );
    }
  }

  /**
   * Helper. Returns a stream of actions to get source data and write a single file after obtaining data.
   */
  generateOutputFile(
    output: fromOutput.OutputState,
    treeBySourceId: StringTMap<RavenSource>,
    customFiltersBySourceId: StringTMap<RavenCustomFilter[]>,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
  ) {
    const actions: Observable<Action>[] = [];
    let keepHeader = true;

    actions.push(
      of(new outputActions.UpdateOutputSettings({ outputData: '' })),
    );

    Object.keys(output.outputSourceIdsByLabel).forEach(label => {
      const sourceIds = output.outputSourceIdsByLabel[label];
      sourceIds.forEach(sourceId => {
        const source = treeBySourceId[sourceId];
        actions.push(
          this.getCsvDataForSource(
            output,
            treeBySourceId,
            getCustomFilterForLabel(label, customFiltersBySourceId[sourceId]),
            filtersByTarget,
            source,
            keepHeader,
          ),
        );
        keepHeader = false;
      });
    });

    actions.push(of(new outputActions.WriteFile()));

    return actions;
  }

  /**
   * Helper. Returns a stream of actions to get and write file for each source.
   */
  generateOutputFiles(
    output: fromOutput.OutputState,
    treeBySourceId: StringTMap<RavenSource>,
    customFiltersBySourceId: StringTMap<RavenCustomFilter[]>,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
  ) {
    const actions: Observable<Action>[] = [];

    Object.keys(output.outputSourceIdsByLabel).forEach(label => {
      const sourceIds = output.outputSourceIdsByLabel[label];
      sourceIds.forEach(sourceId => {
        const source = treeBySourceId[sourceId];
        actions.push(
          this.createFileForSource(
            output,
            treeBySourceId,
            getCustomFilterForLabel(label, customFiltersBySourceId[sourceId]),
            filtersByTarget,
            source,
            label,
          ),
        );
      });
    });

    return actions;
  }

  /**
   * Helper. Get CSV data for source and append it to the output data in the store.
   */
  getCsvDataForSource(
    output: fromOutput.OutputState,
    treeBySourceId: StringTMap<RavenSource>,
    customFilter: RavenCustomFilter | null,
    filtersByTarget: StringTMap<StringTMap<string[]>>,
    source: RavenSource,
    keepHeader: boolean,
  ) {
    const outputDataUrl = getOutputDataUrl(
      treeBySourceId,
      source,
      customFilter,
      filtersByTarget,
      output.outputFormat,
      output.decimateOutputData,
    );

    return this.http.get(outputDataUrl, { responseType: 'text' }).pipe(
      map(dataWithHeader => this.sanitizeData(dataWithHeader, keepHeader)),
      switchMap(data => of(new outputActions.AppendData(data))),
    );
  }

  /**
   * Helper. Remove the first line in the csv data. The first line in CSV data from MPSServer is the CSV header.
   */
  removeCsvHeader(data: string) {
    return data.substring(data.indexOf('\n') + 1);
  }

  /**
   * Helper. Removes header if !keepHeader and ensure data ends with '\n'.
   */
  sanitizeData(dataWithHeader: string, keepHeader: boolean) {
    const data = keepHeader
      ? dataWithHeader
      : this.removeCsvHeader(dataWithHeader);
    return data.endsWith('\n') ? data : `${data}\n`;
  }

  /**
   * Helper. Save to file. Append file ext. if not already in filename.
   */
  writeToFile(data: any, name: string, type: string) {
    const filename = name.endsWith(type) ? name : `${name}.${type}`;
    const blob = new Blob([data], { type: 'text/plain' });
    saveAs(blob, filename);
  }
}
