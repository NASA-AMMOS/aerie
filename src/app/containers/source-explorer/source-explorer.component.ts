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

import { MatDialog } from '@angular/material';

import { Store } from '@ngrx/store';

import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/Subject';

import * as fromSourceExplorer from './../../reducers/source-explorer';

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

      // TODO. Find out how to remove this checking.
      this.changeDetector.markForCheck();
      setTimeout(() =>
        this.changeDetector.detectChanges(),
      );
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

    if (event === 'delete') {
      this.openDeleteDialog(source);
    } else if (event === 'load') {
      this.openLoadDialog(source);
    } else if (event === 'save') {
      this.openSaveDialog(source);
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
   * Dialog trigger. Opens the delete dialog.
   */
  openDeleteDialog(source: RavenSource) {
    const stateDeleteDialog = this.dialog.open(RavenConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: 'Are you sure you want to delete this source?',
      },
      width: '250px',
    });

    stateDeleteDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result.confirm) {
        this.store.dispatch(new sourceExplorerActions.RemoveSourceEvent(source));
      }
    });
  }

  /**
   * Dialog trigger. Opens the load dialog.
   */
  openLoadDialog(source: RavenSource) {
    const stateLoadDialog = this.dialog.open(RavenConfirmDialogComponent, {
      data: {
        cancelText: 'No',
        confirmText: 'Yes',
        message: 'Applying this state will clear your current workspace. Are you sure you want to do this?',
      },
      width: '250px',
    });

    stateLoadDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result.confirm) {
        this.store.dispatch(new sourceExplorerActions.LoadFromSource(source.url));
      }
    });
  }

  /**
   * Dialog trigger. Opens the save state dialog.
   */
  openSaveDialog(source: RavenSource): void {
    const stateSaveDialog = this.dialog.open(RavenStateSaveDialogComponent, {
      data: { source },
      width: '250px',
    });

    stateSaveDialog.afterClosed().pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(result => {
      if (result.save) {
        this.store.dispatch(new sourceExplorerActions.SaveToSource(source, result.name));
      }
    });
  }
}
