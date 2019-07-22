/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import keyBy from 'lodash-es/keyBy';
import { of, zip } from 'rxjs';
import { exhaustMap, map, withLatestFrom } from 'rxjs/operators';
import { NestConfirmDialogComponent } from '../../shared/components/nest-confirm-dialog/nest-confirm-dialog.component';
import {
  DialogActions,
  EpochsActions,
  SourceExplorerActions,
  TimelineActions,
} from '../actions';
import { RavenCustomFilterDialogComponent } from '../components/raven-custom-filter-dialog/raven-custom-filter-dialog.component';
import { RavenCustomGraphDialogComponent } from '../components/raven-custom-graph-dialog/raven-custom-graph-dialog.component';
import { RavenFileImportDialogComponent } from '../components/raven-file-import-dialog/raven-file-import-dialog.component';
import { RavenFolderDialogComponent } from '../components/raven-folder-dialog/raven-folder-dialog.component';
import { RavenLoadEpochDialogComponent } from '../components/raven-load-epoch-dialog/raven-load-epoch-dialog.component';
import { RavenPinDialogComponent } from '../components/raven-pin-dialog/raven-pin-dialog.component';
import { RavenSaveNewEpochFileDialogComponent } from '../components/raven-save-new-epoch-file-dialog/raven-save-new-epoch-file-dialog.component';
import { RavenSettingsBandsDialogComponent } from '../components/raven-settings-bands-dialog/raven-settings-bands-dialog.component';
import { RavenShareableLinkDialogComponent } from '../components/raven-shareable-link-dialog/raven-shareable-link-dialog.component';
import { RavenStateSaveDialogComponent } from '../components/raven-state-save-dialog/raven-state-save-dialog.component';
import { RavenAppState } from '../raven-store';

@Injectable()
export class DialogEffects {
  constructor(
    private actions: Actions,
    private dialog: MatDialog,
    private store: Store<RavenAppState>,
  ) {}

  openApplyCurrentStateDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openApplyCurrentStateDialog),
      exhaustMap(() => {
        const applyCurrentStateDialog = this.dialog.open(
          NestConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: 'Are you sure you want to apply current state?',
            },
            width: '400px',
          },
        );

        return applyCurrentStateDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.confirm) {
          return of(SourceExplorerActions.applyCurrentState());
        }
        return [];
      }),
    ),
  );

  openRemoveAllBandsDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openRemoveAllBandsDialog),
      exhaustMap(action => {
        const removeAllBandsDialog = this.dialog.open(
          NestConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: `Are you sure you want to remove all bands?`,
            },
            width: action.width,
          },
        );

        return removeAllBandsDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.confirm) {
          return of(TimelineActions.removeAllBands());
        }
        return [];
      }),
    ),
  );

  openConfirmDialog = createEffect(
    () =>
      this.actions.pipe(
        ofType(DialogActions.openConfirmDialog),
        exhaustMap(action => {
          this.dialog.open(NestConfirmDialogComponent, {
            data: {
              cancelText: action.cancelText,
              message: action.message,
            },
            width: action.width,
          });
          return [];
        }),
      ),
    { dispatch: false },
  );

  openCustomFilterDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openCustomFilterDialog),
      exhaustMap(action => {
        const customFilterDialog = this.dialog.open(
          RavenCustomFilterDialogComponent,
          {
            data: {
              currentFilter: action.source.filter,
              source: action.source,
              width: action.width,
            },
          },
        );

        return zip(of(action), customFilterDialog.afterClosed());
      }),
      map(([action, result]) => ({ action, result })),
      exhaustMap(({ action, result }) => {
        if (result) {
          return of(
            SourceExplorerActions.setCustomFilter({
              filter: result.filter,
              source: action.source,
            }),
          );
        }
        return [];
      }),
    ),
  );

  openCustomGraphDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openCustomGraphDialog),
      exhaustMap(action => {
        const customGraphableDialog = this.dialog.open(
          RavenCustomGraphDialogComponent,
          {
            data: {
              source: action.source,
            },
            width: '300px',
          },
        );

        return zip(of(action), customGraphableDialog.afterClosed());
      }),
      map(([action, result]) => ({ action, result })),
      exhaustMap(({ action, result }) => {
        if (result && result.label) {
          return of(
            SourceExplorerActions.graphCustomSource({
              filter: result.filter,
              label: result.label,
              sourceId: action.source.id,
            }),
          );
        }
        return [];
      }),
    ),
  );

  openDeleteBandDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openDeleteBandDialog),
      exhaustMap(action => {
        const deleteSubBandDialog = this.dialog.open(
          NestConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: 'Are you sure you want to delete this band?',
            },
            width: action.width,
          },
        );

        return zip(of(action), deleteSubBandDialog.afterClosed());
      }),
      map(([action, result]) => ({ action, result })),
      exhaustMap(({ action: { band }, result }) => {
        if (result && result.confirm) {
          const actions = [];
          for (let i = 0, l = band.subBands.length; i < l; ++i) {
            actions.push(
              TimelineActions.removeSubBand({ subBandId: band.subBands[i].id }),
            );
            actions.push(
              SourceExplorerActions.subBandIdRemove({
                sourceIds: band.subBands[i].sourceIds,
                subBandId: band.subBands[i].id,
              }),
            );
          }
          return actions;
        }
        return [];
      }),
    ),
  );

  openDeleteSourceDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openDeleteSourceDialog),
      exhaustMap(action => {
        const deleteDialog = this.dialog.open(NestConfirmDialogComponent, {
          data: {
            cancelText: 'No',
            confirmText: 'Yes',
            message: `Are you sure you want to delete ${action.source.name}?`,
          },
          width: action.width,
        });

        return zip(of(action), deleteDialog.afterClosed());
      }),
      map(([action, result]) => ({ action, result })),
      exhaustMap(({ action, result }) => {
        if (result && result.confirm) {
          return of(
            SourceExplorerActions.removeSourceEvent({ source: action.source }),
          );
        }
        return [];
      }),
    ),
  );

  openFileImportDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openFileImportDialog),
      exhaustMap(action => {
        const fileImportDialog = this.dialog.open(
          RavenFileImportDialogComponent,
          {
            data: {
              source: action.source,
            },
            width: action.width,
          },
        );

        return zip(of(action), fileImportDialog.afterClosed());
      }),
      map(([action, result]) => ({ action, result })),
      exhaustMap(({ action, result }) => {
        if (result && result.import) {
          return of(
            SourceExplorerActions.importFile({
              file: result.file,
              source: action.source,
            }),
          );
        }
        return [];
      }),
    ),
  );

  openFolderDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openFolderDialog),
      exhaustMap(action => {
        const folderDialog = this.dialog.open(RavenFolderDialogComponent, {
          data: {
            source: action.source,
            type: action.folderAction,
          },
          width: action.width,
        });
        return folderDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.folderAdd) {
          return of(SourceExplorerActions.folderAdd({ folder: result.folder }));
        }
        return [];
      }),
    ),
  );

  openLoadEpochDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openLoadEpochDialog),
      exhaustMap(action => {
        const openLoadEpochDialog = this.dialog.open(
          RavenLoadEpochDialogComponent,
          {
            data: {
              sourceUrl: action.sourceUrl,
            },
            width: '600px',
          },
        );
        return openLoadEpochDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.replaceAction) {
          return of(
            EpochsActions.fetchEpochs({
              replaceAction: result.replaceAction,
              url: result.sourceUrl,
            }),
          );
        }
        return [];
      }),
    ),
  );

  /**
   * @note In this effect we have two separate `PinAdd`, `PinRemove`, and `PinRename` actions for the
   * timeline and source explorer reducers. This is to keep these reducers as decoupled as possible.
   * Because they are separate, make sure their call order is maintained (e.g. source-explorer PinAdd is first followed by timeline PinAdd).
   * This way the timeline effect has the new pins to work with when we get there.
   */
  openPinDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openPinDialog),
      withLatestFrom(this.store),
      map(([action, state]) => ({ action, state })),
      exhaustMap(({ action, state: { raven } }) => {
        const pinDialog = this.dialog.open(RavenPinDialogComponent, {
          data: {
            pin: raven.sourceExplorer.pins.find(
              p => p.sourceId === action.source.id,
            ),
            source: action.source,
            type: action.pinAction,
          },
          width: action.width,
        });

        return pinDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.pinAdd) {
          return [
            SourceExplorerActions.pinAdd({ pin: result.pin }),
            TimelineActions.pinAdd({ pin: result.pin }),
          ];
        } else if (result && result.pinRemove) {
          return [
            SourceExplorerActions.pinRemove({ sourceId: result.sourceId }),
            TimelineActions.pinRemove({ sourceId: result.sourceId }),
          ];
        } else if (result && result.pinRename) {
          return [
            SourceExplorerActions.pinRename({
              newName: result.newName,
              sourceId: result.sourceId,
            }),
            TimelineActions.pinRename({
              newName: result.newName,
              sourceId: result.sourceId,
            }),
          ];
        }
        return [];
      }),
    ),
  );

  openRemoveAllGuidesDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openRemoveAllGuidesDialog),
      exhaustMap(action => {
        const removeAllGuidesDialog = this.dialog.open(
          NestConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: 'Are you sure you want to remove all guides?',
            },
            width: action.width,
          },
        );

        return removeAllGuidesDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.confirm) {
          return of(TimelineActions.removeAllGuides());
        }
        return [];
      }),
    ),
  );

  openSaveNewEpochFileDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openSaveNewEpochFileDialog),
      exhaustMap(() => {
        const saveNewEpochFileDialog = this.dialog.open(
          RavenSaveNewEpochFileDialogComponent,
          {
            data: {
              cancelText: 'Cancel',
            },
            width: '400px',
          },
        );
        return saveNewEpochFileDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.filePathName) {
          return of(
            EpochsActions.saveNewEpochFile({
              filePathName: result.filePathName,
            }),
          );
        }
        return [];
      }),
    ),
  );

  openSettingsBandDialog = createEffect(
    () =>
      this.actions.pipe(
        ofType(DialogActions.openSettingsBandDialog),
        withLatestFrom(this.store),
        map(([action, state]) => ({ action, state })),
        exhaustMap(({ action, state: { raven } }) => {
          this.dialog.open(RavenSettingsBandsDialogComponent, {
            data: {
              bandsById: keyBy(raven.timeline.bands, 'id'),
              selectedBandId: action.bandId,
              selectedSubBandId: action.subBandId,
            },
            width: '450px',
          });
          return [];
        }),
      ),
    { dispatch: false },
  );

  openShareableLinkDialog = createEffect(
    () =>
      this.actions.pipe(
        ofType(DialogActions.openShareableLinkDialog),
        withLatestFrom(this.store),
        map(([action, state]) => ({ action, state })),
        exhaustMap(({ action, state }) => {
          this.dialog.open(RavenShareableLinkDialogComponent, {
            data: {
              state,
            },
            width: action.width,
          });
          return [];
        }),
      ),
    { dispatch: false },
  );

  openStateApplyDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openStateApplyDialog),
      exhaustMap(action => {
        const applyStateDialog = this.dialog.open(NestConfirmDialogComponent, {
          data: {
            cancelText: 'No',
            confirmText: 'Yes',
            message:
              'Applying this state will clear your current workspace. Are you sure you want to do this?',
          },
          width: action.width,
        });

        return zip(of(action), applyStateDialog.afterClosed());
      }),
      map(([action, result]) => ({ action, result })),
      exhaustMap(({ action, result }) => {
        if (result && result.confirm) {
          return of(
            SourceExplorerActions.applyState({
              sourceId: action.source.id,
              sourceUrl: action.source.url,
            }),
          );
        }
        return [];
      }),
    ),
  );

  openStateSaveDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openStateSaveDialog),
      exhaustMap(action => {
        const stateSaveDialog = this.dialog.open(
          RavenStateSaveDialogComponent,
          {
            data: {
              source: action.source,
            },
            width: action.width,
          },
        );

        return zip(of(action), stateSaveDialog.afterClosed());
      }),
      map(([action, result]) => ({ action, result })),
      exhaustMap(({ action, result }) => {
        if (result && result.save) {
          return of(
            SourceExplorerActions.saveState({
              name: result.name,
              source: action.source,
            }),
          );
        }
        return [];
      }),
    ),
  );

  openUpdateCurrentStateDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openUpdateCurrentStateDialog),
      exhaustMap(() => {
        const updateCurrentStateDialog = this.dialog.open(
          NestConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: 'Are you sure you want to update current state?',
            },
            width: '400px',
          },
        );

        return updateCurrentStateDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.confirm) {
          return of(SourceExplorerActions.updateCurrentState());
        }
        return [];
      }),
    ),
  );

  openUpdateProjectEpochsDialog = createEffect(() =>
    this.actions.pipe(
      ofType(DialogActions.openUpdateProjectEpochsDialog),
      withLatestFrom(this.store),
      map(([, state]) => state),
      exhaustMap(state => {
        let epochPath = '';
        const match = state.config.mpsServer.epochsUrl.match(
          new RegExp('.*/(fs|fs-mongodb)/(.*)'),
        );
        if (match) {
          const [, , path] = match;
          epochPath = path;
        }
        const updateProjectEpochDialog = this.dialog.open(
          NestConfirmDialogComponent,
          {
            data: {
              cancelText: 'No',
              confirmText: 'Yes',
              message: `Are you sure you want to update project epochs in repository: ${epochPath}?`,
            },
            width: '400px',
          },
        );
        return updateProjectEpochDialog.afterClosed();
      }),
      exhaustMap(result => {
        if (result && result.confirm) {
          return of(EpochsActions.updateProjectEpochs());
        }
        return [];
      }),
    ),
  );
}
