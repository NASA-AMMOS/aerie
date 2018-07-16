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

import * as dialogActions from './../../actions/dialog';
import * as epochsActions from './../../actions/epochs';
import * as sourceExplorerActions from './../../actions/source-explorer';

import {
  RavenCustomFilterSource,
  RavenCustomGraphableSource,
  RavenFilterSource,
  RavenGraphableFilterSource,
  RavenPin,
  RavenSource,
  RavenSourceActionEvent,
  StringTMap,
} from './../../shared/models';

import {
  getSortedChildIds,
} from './../../shared/util';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-source-explorer',
  styleUrls: ['./source-explorer.component.css'],
  templateUrl: './source-explorer.component.html',
})
export class SourceExplorerComponent implements OnDestroy {
  // Source Explorer state.
  filtersByTarget: StringTMap<StringTMap<string[]>> | null;
  pins: RavenPin[];
  selectedSourceId: string;
  tree: StringTMap<RavenSource>;

  // Local state (derived on Source Explorer state).
  sortedChildIds: string[];

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(
    private changeDetector: ChangeDetectorRef,
    private store: Store<fromSourceExplorer.SourceExplorerState>,
  ) {
    // Source Explorer state.
    this.store.select(fromSourceExplorer.getFiltersByTarget).pipe(
      takeUntil(this.ngUnsubscribe),
    ).subscribe(filtersByTarget => {
      this.filtersByTarget = filtersByTarget;
      this.markForCheck();
    });
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
      this.sortedChildIds = getSortedChildIds(this.tree, this.tree['/'].childIds);
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
        const match = data.subject.match(new RegExp('(.*/fs-mongodb)(/.*)/(.*)'));
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
      this.store.dispatch(new dialogActions.OpenLayoutApplyDialog(source, '250px'));
    } else if (event === 'apply-state') {
      this.store.dispatch(new dialogActions.OpenStateApplyDialog(source, '250px'));
    } else if (event === 'delete') {
      this.store.dispatch(new dialogActions.OpenDeleteDialog(source, '250px'));
    } else if (event === 'epoch-load') {
      this.onLoadEpochs(source);
    } else if (event === 'file-import') {
      this.store.dispatch(new dialogActions.OpenFileImportDialog(source, '300px'));
    } else if (event === 'pin-add') {
      this.store.dispatch(new dialogActions.OpenPinDialog('add', source, '250px'));
    } else if (event === 'pin-remove') {
      this.store.dispatch(new dialogActions.OpenPinDialog('remove', source, '250px'));
    } else if (event === 'pin-rename') {
      this.store.dispatch(new dialogActions.OpenPinDialog('rename', source, '250px'));
    } else if (event === 'save') {
      this.store.dispatch(new dialogActions.OpenStateSaveDialog(source, '300px'));
    }
  }

  /**
   * Event. Called when a custom graphable source is clicked.
   */
  onAddCustomGraph(source: RavenCustomGraphableSource): void {
    this.store.dispatch(new dialogActions.OpenCustomGraphDialog(source, '300px'));
  }

  /**
   * Event. Called when a filter is selected
   */
  onAddFilter(source: RavenFilterSource): void {
    this.store.dispatch(new sourceExplorerActions.AddFilter(source));
  }

  /**
   * Event. Called when a graphable filter is selected.
   */
  onAddGraphableFilter(source: RavenGraphableFilterSource): void {
    this.store.dispatch(new sourceExplorerActions.AddGraphableFilter(source));
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
   * Event. Called when a filter is unselected
   */
  onRemoveFilter(source: RavenFilterSource): void {
    this.store.dispatch(new sourceExplorerActions.RemoveFilter(source));
  }

  /**
   * Event. Called when a graphable filter is unselected.
   */
  onRemoveGraphableFilter(source: RavenGraphableFilterSource): void {
    this.store.dispatch(new sourceExplorerActions.RemoveGraphableFilter(source));
  }

  /**
   * Event. Called when a `select` event is fired from a raven-tree.
   */
  onSelect(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SelectSource(source));
  }

  /**
   * Event. Called when a custom filter source is clicked.
   */
  onSelectCustomFilter(source: RavenCustomFilterSource): void {
    this.store.dispatch(new dialogActions.OpenCustomFilterDialog(source, '300px'));
  }
}
