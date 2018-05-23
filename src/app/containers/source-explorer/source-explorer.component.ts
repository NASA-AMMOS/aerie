/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnDestroy,
} from '@angular/core';

import {
  MatDialog,
} from '@angular/material';

import {
  Store,
} from '@ngrx/store';

import {
  switchMap,
  takeUntil,
} from 'rxjs/operators';

import {
  Subject,
} from 'rxjs';

import {
  WebSocketSubject,
} from 'rxjs/webSocket';

import * as fromConfig from './../../reducers/config';
import * as fromSourceExplorer from './../../reducers/source-explorer';

import * as epochsActions from './../../actions/epochs';
import * as sourceExplorerActions from './../../actions/source-explorer';
import * as timelineActions from './../../actions/timeline';

import {
  RavenConfirmDialogComponent,
  RavenFileImportDialogComponent,
  RavenLayoutApplyDialogComponent,
  RavenPinDialogComponent,
  RavenStateSaveDialogComponent,
} from './../../shared/raven/components';

import {
  RavenPin,
  RavenSource,
  RavenSourceActionEvent,
  StringTMap,
} from './../../shared/models';

import {
  getAllSourcesByKind,
} from './../../shared/util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-source-explorer',
  styleUrls: ['./source-explorer.component.css'],
  templateUrl: './source-explorer.component.html',
})
export class SourceExplorerComponent implements OnDestroy {
  // Source Explorer state.
  pins: RavenPin[];
  selectedSourceId: string;
  tree: StringTMap<RavenSource>;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private dialog: MatDialog,
    private store: Store<fromSourceExplorer.SourceExplorerState>,
  ) {
    // Source Explorer state.
    this.store.select(fromSourceExplorer.getPins).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(pins => {
      this.pins = pins;
      this.markForCheck();
    });
    this.store.select(fromSourceExplorer.getSelectedSourceId).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(selectedSourceId => {
      this.selectedSourceId = selectedSourceId;
      this.markForCheck();
    });
    this.store.select(fromSourceExplorer.getTreeBySourceId).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(tree => {
      this.tree = tree;
      this.markForCheck();
    });

    // Connect to web socket to update new sources when they change on the server.
    this.connectToWebsocket();
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Helper that connects to the MPS Server websocket.
   * When a data sources changes we fetch new sources to update the source explorer.
   */
  connectToWebsocket() {
    this.store.select(fromConfig.getConfigState).pipe(
      switchMap(config =>
        WebSocketSubject.create(`${config.baseUrl.replace('https', 'wss')}/${config.baseSocketUrl}`),
      ),
      takeUntil(this.ngUnsubscribe),
    ).subscribe((data: any) => {
      if (data.detail === 'data source changed') {
        const pattern = new RegExp('(.*/fs-mongodb)(/.*)/(.*)');
        const match = data.subject.match(pattern);
        const sourceId = `${match[2]}`;
        const sourceUrl = `${match[1]}${match[2]}`;
        this.store.dispatch(new sourceExplorerActions.FetchNewSources(sourceId, sourceUrl));
      }
    });
  }

  /**
   * Helper. Marks this component for change detection check,
   * and then detects changes on the next tick.
   *
   * TODO: Find out how we can remove this.
   */
  markForCheck() {
    this.changeDetector.markForCheck();
    setTimeout(() => this.changeDetector.detectChanges());
  }

  /**
   * Event. Called when an `action` event is fired from the raven-tree.
   */
  onAction(action: RavenSourceActionEvent): void {
    const { event, source } = action;

    if (event === 'apply-layout') {
      this.openApplyLayoutDialog(source);
    } else if (event === 'apply-state') {
      this.openApplyStateDialog(source);
    } else if (event === 'delete') {
      this.openDeleteDialog(source);
    } else if (event === 'epoch-load') {
      this.onLoadEpochs(source);
    } else if (event === 'file-import') {
      this.openFileImportDialog(source);
    } else if (event === 'pin-add') {
      this.openPinDialog('add', source);
    } else if (event === 'pin-remove') {
      this.openPinDialog('remove', source);
    } else if (event === 'pin-rename') {
      this.openPinDialog('rename', source);
    } else if (event === 'save') {
      this.openStateSaveDialog(source);
    }
  }

  /**
   * Event. Called when a `close` event is fired from a raven-tree.
   */
  onClose(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.CloseEvent(source.id));
  }

  /**
   * Event. Called when a `collapse` event is fired from a raven-tree.
   */
  onCollapse(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.CollapseEvent(source.id));
  }

  /**
   * Event. Called when an `expand` event is fired from a raven-tree.
   */
  onExpand(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.ExpandEvent(source.id));
  }

  /**
   * Event. Called when a `load-epoch` event is caught from the source action menu.
   */
  onLoadEpochs(source: RavenSource): void {
    this.store.dispatch(new epochsActions.FetchEpochs(source.url));
  }

  /**
   * Event. Called when an `open` event is fired from a raven-tree.
   */
  onOpen(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.OpenEvent(source.id));
  }

  /**
   * Event. Called when a `select` event is fired from a raven-tree.
   */
  onSelect(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SelectSource(source));
  }

  /**
   * Dialog trigger. Opens the apply layout dialog.
   */
  openApplyLayoutDialog(source: RavenSource) {
    const applyLayoutDialog = this.dialog.open(RavenLayoutApplyDialogComponent, {
      data: {
        sources: getAllSourcesByKind(this.tree, '/', 'fs_file'),
      },
      width: '250px',
    });

    applyLayoutDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result && result.apply) {
        this.store.dispatch(new sourceExplorerActions.ApplyLayout(source.url, source.id, result.sourceId));
      }
    });
  }

  /**
   * Dialog trigger. Opens the apply state dialog.
   */
  openApplyStateDialog(source: RavenSource) {
    const applyStateDialog = this.dialog.open(RavenConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: 'Applying this state will clear your current workspace. Are you sure you want to do this?',
      },
      width: '250px',
    });

    applyStateDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result && result.confirm) {
        this.store.dispatch(new sourceExplorerActions.ApplyState(source.url, source.id));
      }
    });
  }

  /**
   * Dialog trigger. Opens the delete dialog.
   */
  openDeleteDialog(source: RavenSource) {
    const stateDeleteDialog = this.dialog.open(RavenConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: `Are you sure you want to delete ${source.name}?`,
      },
      width: '250px',
    });

    stateDeleteDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result && result.confirm) {
        this.store.dispatch(new sourceExplorerActions.RemoveSourceEvent(source));
      }
    });
  }

  /**
   * Dialog trigger. Opens the file import dialog.
   */
  openFileImportDialog(source: RavenSource): void {
    const fileImportDialog = this.dialog.open(RavenFileImportDialogComponent, {
      data: { source },
      width: '300px',
    });

    fileImportDialog.afterClosed().subscribe(result => {
      if (result && result.import) {
        this.store.dispatch(new sourceExplorerActions.ImportFile(source, result.file));
      }
    });
  }

  /**
   * Dialog trigger. Opens the pin dialog.
   * NOTE: In this function we have two separate `PinAdd`, `PinRemove`, and `PinRename` actions for the
   *       timeline and source explorer reducers. This is to keep these reducers as decoupled as possible.
   *       Because they are separate, make sure their call order is maintained (e.g. source-explorer PinAdd is first followed by timeline PinAdd).
   *       This way the timeline effect has the new pins to work with when we get there.
   */
  openPinDialog(type: string, source: RavenSource): void {
    const pinDialog = this.dialog.open(RavenPinDialogComponent, {
      data: {
        pin: this.pins.find(p => p.sourceId === source.id),
        source,
        type,
      },
      width: '250px',
    });

    pinDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result && result.pinAdd) {
        this.store.dispatch(new sourceExplorerActions.PinAdd(result.pin));
        this.store.dispatch(new timelineActions.PinAdd(result.pin));
      } else if (result && result.pinRemove) {
        this.store.dispatch(new sourceExplorerActions.PinRemove(result.sourceId));
        this.store.dispatch(new timelineActions.PinRemove(result.sourceId));
      } else if (result && result.pinRename) {
        this.store.dispatch(new sourceExplorerActions.PinRename(result.sourceId, result.newName));
        this.store.dispatch(new timelineActions.PinRename(result.sourceId, result.newName));
      }
    });
  }

  /**
   * Dialog trigger. Opens the save state dialog.
   */
  openStateSaveDialog(source: RavenSource): void {
    const stateSaveDialog = this.dialog.open(RavenStateSaveDialogComponent, {
      data: { source },
      width: '250px',
    });

    stateSaveDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result && result.save) {
        this.store.dispatch(new sourceExplorerActions.SaveState(source, result.name));
      }
    });
  }
}
