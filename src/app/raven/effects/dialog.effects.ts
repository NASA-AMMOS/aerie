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
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable, of, zip } from 'rxjs';
import { exhaustMap, map, withLatestFrom } from 'rxjs/operators';
import { RavenAppState } from '../raven-store';

import {
  RavenConfirmDialogComponent,
  RavenCustomFilterDialogComponent,
  RavenCustomGraphDialogComponent,
  RavenFileImportDialogComponent,
  RavenFolderDialogComponent,
  RavenPinDialogComponent,
  RavenShareableLinkDialogComponent,
  RavenStateSaveDialogComponent,
} from '../../shared/components/components';

import {
  DialogActionTypes,
  OpenConfirmDialog,
  OpenCustomFilterDialog,
  OpenCustomGraphDialog,
  OpenDeleteDialog,
  OpenDeleteSubBandDialog,
  OpenFileImportDialog,
  OpenFolderDialog,
  OpenPinDialog,
  OpenRemoveAllBandsDialog,
  OpenRemoveAllGuidesDialog,
  OpenShareableLinkDialog,
  OpenStateApplyDialog,
  OpenStateSaveDialog,
} from '../actions/dialog.actions';

import * as sourceExplorerActions from '../actions/source-explorer.actions';
import * as timelineActions from '../actions/timeline.actions';

@Injectable()
export class DialogEffects {
  constructor(
    private actions$: Actions,
    private dialog: MatDialog,
    private store$: Store<RavenAppState>,
  ) {}

  /**
   * Effect for OpenRemoveAllBandsDialog.
   */
  @Effect()
  openRemoveAllBandsDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenRemoveAllBandsDialog>(
      DialogActionTypes.OpenRemoveAllBandsDialog,
    ),
    exhaustMap(action => {
      const removeAllBandsDialog = this.dialog.open(
        RavenConfirmDialogComponent,
        {
          data: {
            cancelText: 'No',
            confirmText: 'Yes',
            message: `Are you sure you want to remove all bands?`,
          },
          width: action.width,
        },
      );

      return zip(of(action), removeAllBandsDialog.afterClosed());
    }),
    map(([action, result]) => ({ action, result })),
    exhaustMap(({ action, result }) => {
      if (result && result.confirm) {
        return of(new timelineActions.RemoveAllBands());
      }
      return [];
    }),
  );

  /**
   * Effect for OpenConfirmDialog.
   */
  @Effect({ dispatch: false })
  openConfirmDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenConfirmDialog>(DialogActionTypes.OpenConfirmDialog),
    exhaustMap(action => {
      this.dialog.open(RavenConfirmDialogComponent, {
        data: {
          cancelText: action.cancelText,
          message: action.message,
        },
        width: action.width,
      });
      return [];
    }),
  );

  /**
   * Effect for OpenCustomFilterDialog.
   */
  @Effect()
  openCustomFilterDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenCustomFilterDialog>(DialogActionTypes.OpenCustomFilterDialog),
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
          new sourceExplorerActions.SetCustomFilter(
            action.source,
            result.filter,
          ),
        );
      }
      return [];
    }),
  );

  /**
   * Effect for OpenCustomGraphDialog.
   */
  @Effect()
  openCustomGraphDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenCustomGraphDialog>(DialogActionTypes.OpenCustomGraphDialog),
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
          new sourceExplorerActions.GraphCustomSource(
            action.source.id,
            result.label,
            result.filter,
          ),
        );
      }
      return [];
    }),
  );

  /**
   * Effect for OpenDeleteDialog.
   */
  @Effect()
  openDeleteDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenDeleteDialog>(DialogActionTypes.OpenDeleteDialog),
    exhaustMap(action => {
      const deleteDialog = this.dialog.open(RavenConfirmDialogComponent, {
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
        return of(new sourceExplorerActions.RemoveSourceEvent(action.source));
      }
      return [];
    }),
  );

  /**
   * Effect for OpenDeleteSubBandDialog.
   */
  @Effect()
  openDeleteSubBandDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenDeleteSubBandDialog>(DialogActionTypes.OpenDeleteSubBandDialog),
    exhaustMap(action => {
      const deleteSubBandDialog = this.dialog.open(
        RavenConfirmDialogComponent,
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
    exhaustMap(({ action: { subBand }, result }) => {
      if (result && result.confirm) {
        return [
          new timelineActions.RemoveSubBand(subBand.id),
          new sourceExplorerActions.SubBandIdRemove(
            subBand.sourceIds,
            subBand.id,
          ),
        ];
      }
      return [];
    }),
  );

  /**
   * Effect for OpenFileImportDialog.
   */
  @Effect()
  openFileImportDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenFileImportDialog>(DialogActionTypes.OpenFileImportDialog),
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
          new sourceExplorerActions.ImportFile(action.source, result.file),
        );
      }
      return [];
    }),
  );
  
  /**
   * Effect for OpenFolderDialog.
   */
  @Effect()
  openFolderDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenFolderDialog>(DialogActionTypes.OpenFolderDialog),
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
    exhaustMap((result: any) => {
      if (result && result.folderAdd) {
        return [new sourceExplorerActions.FolderAdd(result.folder) as Action];
      }
      return [];
    }),
  );

  /**
   * Effect for OpenPinDialog.
   * NOTE: In this effect we have two separate `PinAdd`, `PinRemove`, and `PinRename` actions for the
   *       timeline and source explorer reducers. This is to keep these reducers as decoupled as possible.
   *       Because they are separate, make sure their call order is maintained (e.g. source-explorer PinAdd is first followed by timeline PinAdd).
   *       This way the timeline effect has the new pins to work with when we get there.
   */
  @Effect()
  openPinDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenPinDialog>(DialogActionTypes.OpenPinDialog),
    withLatestFrom(this.store$),
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
    exhaustMap((result: any) => {
      if (result && result.pinAdd) {
        return [
          new sourceExplorerActions.PinAdd(result.pin) as Action,
          new timelineActions.PinAdd(result.pin) as Action,
        ];
      } else if (result && result.pinRemove) {
        return [
          new sourceExplorerActions.PinRemove(result.sourceId) as Action,
          new timelineActions.PinRemove(result.sourceId) as Action,
        ];
      } else if (result && result.pinRename) {
        return [
          new sourceExplorerActions.PinRename(
            result.sourceId,
            result.newName,
          ) as Action,
          new timelineActions.PinRename(
            result.sourceId,
            result.newName,
          ) as Action,
        ];
      }
      return [];
    }),
  );

  /**
   * Effect for OpenRemoveAllGuidesDialog.
   */
  @Effect()
  openRemoveAllGuidesDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenRemoveAllGuidesDialog>(
      DialogActionTypes.OpenRemoveAllGuidesDialog,
    ),
    exhaustMap(action => {
      const removeAllGuidesDialog = this.dialog.open(
        RavenConfirmDialogComponent,
        {
          data: {
            cancelText: 'No',
            confirmText: 'Yes',
            message: 'Are you sure you want to remove all guides?',
          },
          width: action.width,
        },
      );

      return zip(of(action), removeAllGuidesDialog.afterClosed());
    }),
    map(([action, result]) => ({ action, result })),
    exhaustMap(({ result }) => {
      if (result && result.confirm) {
        return [new timelineActions.RemoveAllGuides()];
      }
      return [];
    }),
  );

  /**
   * Effect for OpenShareableLinkDialog.
   */
  @Effect({ dispatch: false })
  openShareableLinkDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenShareableLinkDialog>(DialogActionTypes.OpenShareableLinkDialog),
    withLatestFrom(this.store$),
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
  );

  /**
   * Effect for OpenStateApplyDialog.
   */
  @Effect()
  openStateApplyDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenStateApplyDialog>(DialogActionTypes.OpenStateApplyDialog),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    exhaustMap(({ action, state }) => {
      const applyStateDialog = this.dialog.open(RavenConfirmDialogComponent, {
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
          new sourceExplorerActions.ApplyState(
            action.source.url,
            action.source.id,
          ),
        );
      }
      return [];
    }),
  );

  /**
   * Effect for OpenStateApplyDialog.
   */
  @Effect()
  openStateSaveDialog$: Observable<Action> = this.actions$.pipe(
    ofType<OpenStateSaveDialog>(DialogActionTypes.OpenStateSaveDialog),
    withLatestFrom(this.store$),
    map(([action, state]) => ({ action, state })),
    exhaustMap(({ action, state }) => {
      const stateSaveDialog = this.dialog.open(RavenStateSaveDialogComponent, {
        data: {
          source: action.source,
        },
        width: action.width,
      });

      return zip(of(action), stateSaveDialog.afterClosed());
    }),
    map(([action, result]) => ({ action, result })),
    exhaustMap(({ action, result }) => {
      if (result && result.save) {
        return of(
          new sourceExplorerActions.SaveState(action.source, result.name),
        );
      }
      return [];
    }),
  );
}
