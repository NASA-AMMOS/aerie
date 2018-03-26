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

import { Store } from '@ngrx/store';

import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/Subject';

import { MatDialog } from '@angular/material';

import * as fromSourceExplorer from './../../reducers/source-explorer';

import * as displayActions from './../../actions/display';
import * as sourceExplorerActions from './../../actions/source-explorer';

import {
  RavenConfirmDialogComponent,
  RavenStateSaveDialogComponent,
} from './../../components';

import {
  RavenSource,
  RavenSourceActionEvent,
  StringTMap,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-source-explorer',
  styleUrls: ['./source-explorer.component.css'],
  templateUrl: './source-explorer.component.html',
})
export class SourceExplorerComponent implements OnDestroy {
  // Source Explorer state.
  tree: StringTMap<RavenSource>;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private dialog: MatDialog,
    private store: Store<fromSourceExplorer.SourceExplorerState>,
  ) {
    // Source Explorer state.
    this.store.select(fromSourceExplorer.getTreeBySourceId).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(tree => {
      this.tree = tree;
      this.changeDetector.markForCheck();
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  /**
   * Event. Called when an `action` event is fired from the raven-tree.
   */
  onAction(action: RavenSourceActionEvent): void {
    const { event, source } = action;

    if (event === 'state-delete') {
      this.openStateDeleteDialog(source);
    } else if (event === 'state-load') {
      this.openStateLoadDialog(source);
    } else if (event === 'state-save') {
      this.openStateSaveDialog(source);
    }
  }

  /**
   * Event. Called when a `close` event is fired from a raven-tree.
   */
  onClose(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SourceExplorerCloseEvent(source.id));
  }

  /**
   * Event. Called when a `collapse` event is fired from a raven-tree.
   */
  onCollapse(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SourceExplorerCollapseEvent(source.id));
  }

  /**
   * Event. Called when an `expand` event is fired from a raven-tree.
   */
  onExpand(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SourceExplorerExpandEvent(source.id));
  }

  /**
   * Event. Called when an `open` event is fired from a raven-tree.
   */
  onOpen(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SourceExplorerOpenEvent(source.id));
  }

  /**
   * Event. Called when a `select` event is fired from a raven-tree.
   */
  onSelect(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SourceExplorerSelect(source));
  }

  /**
   * Dialog trigger. Opens the delete state dialog.
   */
  openStateDeleteDialog(source: RavenSource) {
    const stateDeleteDialog = this.dialog.open(RavenConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: 'Are you sure you want to delete this state?',
      },
      width: '250px',
    });

    stateDeleteDialog.afterClosed().subscribe(result => {
      if (result.confirm) {
        this.store.dispatch(new displayActions.StateDelete(source));
      }
    });
  }

  /**
   * Dialog trigger. Opens the load state dialog.
   */
  openStateLoadDialog(source: RavenSource) {
    const stateLoadDialog = this.dialog.open(RavenConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: 'Loading this state will clear your current workspace. Are you sure you want to do this?',
      },
      width: '250px',
    });

    stateLoadDialog.afterClosed().subscribe(result => {
      if (result.confirm) {
        this.store.dispatch(new displayActions.StateLoad(source));
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

    stateSaveDialog.afterClosed().subscribe(result => {
      if (result.save) {
        this.store.dispatch(new displayActions.StateSave(result.name, source));
      }
    });
  }
}
