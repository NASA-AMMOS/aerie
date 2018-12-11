/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';

import { select, Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { switchMap, takeUntil } from 'rxjs/operators';
import { WebSocketSubject } from 'rxjs/webSocket';
import { SourceExplorerState } from '../../reducers/source-explorer.reducer';

import {
  RavenCustomFilterSource,
  RavenCustomGraphableSource,
  RavenFilterSource,
  RavenGraphableFilterSource,
  RavenPin,
  RavenSource,
  RavenSourceActionEvent,
  StringTMap,
} from '../../../shared/models';

import { getUrls } from '../../../shared/selectors';

import {
  getFiltersByTarget,
  getPins,
  getSelectedSourceId,
  getTreeBySourceId,
  treeSortedChildIds,
} from '../../selectors';

import * as dialogActions from '../../actions/dialog.actions';
import * as epochsActions from '../../actions/epochs.actions';
import * as layoutActions from '../../actions/layout.actions';
import * as sourceExplorerActions from '../../actions/source-explorer.actions';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-source-explorer',
  styleUrls: ['./source-explorer.component.css'],
  templateUrl: './source-explorer.component.html',
})
export class SourceExplorerComponent implements OnDestroy {
  filtersByTarget$: Observable<StringTMap<StringTMap<string[]>> | null>;
  pins$: Observable<RavenPin[]>;
  selectedSourceId$: Observable<string>;
  sortedChildIds$: Observable<string[]>;
  tree$: Observable<StringTMap<RavenSource>>;

  tree: StringTMap<RavenSource>;

  private ngUnsubscribe: Subject<{}> = new Subject();

  constructor(private store: Store<SourceExplorerState>) {
    this.filtersByTarget$ = this.store.pipe(select(getFiltersByTarget));
    this.pins$ = this.store.pipe(select(getPins));
    this.selectedSourceId$ = this.store.pipe(select(getSelectedSourceId));
    this.sortedChildIds$ = this.store.pipe(select(treeSortedChildIds));
    this.tree$ = this.store.pipe(select(getTreeBySourceId));

    this.tree$
      .pipe(takeUntil(this.ngUnsubscribe))
      .subscribe(tree => (this.tree = tree));

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
    this.store
      .pipe(
        select(getUrls),
        switchMap(
          config =>
            new WebSocketSubject(
              `${config.baseUrl.replace('https', 'wss')}/${config.socketUrl}`,
            ),
        ),
        takeUntil(this.ngUnsubscribe),
      )
      .subscribe((data: any) => {
        if (data.detail === 'data source changed') {
          const match = data.subject.match(
            new RegExp('(.*/fs-mongodb)(/.*)/(.*)'),
          );
          const sourceId = `${match[2]}`;
          const sourceUrl = `${match[1]}${match[2]}`;
          this.store.dispatch(
            new sourceExplorerActions.FetchNewSources(sourceId, sourceUrl),
          );
        } else if (data.aspect === 'importJobStatus') {
          const match = data.subject.match(new RegExp('(.*)/(.*)'));
          const parentId = `${match[1]}`;
          if (this.tree[parentId]) {
            this.store.dispatch(
              new sourceExplorerActions.FetchNewSources(
                parentId,
                this.tree[parentId].url,
              ),
            );
          }
        }
      });
  }

  /**
   * Event. Called when an `action` event is fired from the raven-tree.
   */
  onAction(action: RavenSourceActionEvent): void {
    const { event, source } = action;
    if (event === 'apply-layout') {
      this.store.dispatch(
        new sourceExplorerActions.UpdateSourceExplorer({
          currentStateId: source.id,
        }),
      );
      this.store.dispatch(new layoutActions.ToggleApplyLayoutDrawerEvent(true));
    } else if (event === 'apply-state') {
      this.store.dispatch(
        new dialogActions.OpenStateApplyDialog(source, '250px'),
      );
    } else if (event === 'delete') {
      this.store.dispatch(new dialogActions.OpenDeleteDialog(source, '250px'));
    } else if (event === 'epoch-load') {
      this.onLoadEpochs(source);
    } else if (event === 'file-import') {
      this.store.dispatch(
        new dialogActions.OpenFileImportDialog(source, '300px'),
      );
    } else if (event === 'graph-again') {
      this.store.dispatch(new sourceExplorerActions.GraphAgainEvent(source.id));
    } else if (event === 'pin-add') {
      this.store.dispatch(
        new dialogActions.OpenPinDialog('add', source, '250px'),
      );
    } else if (event === 'pin-remove') {
      this.store.dispatch(
        new dialogActions.OpenPinDialog('remove', source, '250px'),
      );
    } else if (event === 'pin-rename') {
      this.store.dispatch(
        new dialogActions.OpenPinDialog('rename', source, '250px'),
      );
    } else if (event === 'save') {
      this.store.dispatch(
        new dialogActions.OpenStateSaveDialog(source, '300px'),
      );
    }
  }

  /**
   * Event. Called when a custom graphable source is clicked.
   */
  onAddCustomGraph(source: RavenCustomGraphableSource): void {
    this.store.dispatch(
      new dialogActions.OpenCustomGraphDialog(source, '300px'),
    );
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
    this.store.dispatch(
      new sourceExplorerActions.RemoveGraphableFilter(source),
    );
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
    this.store.dispatch(
      new dialogActions.OpenCustomFilterDialog(source, '300px'),
    );
  }
}
