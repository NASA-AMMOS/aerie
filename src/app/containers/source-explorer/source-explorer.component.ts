/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';

import 'rxjs/add/operator/map';
import 'rxjs/add/operator/take';

import * as fromSourceExplorer from './../../reducers/source-explorer';
import * as fromTimeline from './../../reducers/timeline';

import * as sourceExplorerActions from './../../actions/source-explorer';

import {
  removeBandsOrPoints,
  toRavenSources,
} from './../../shared/util';

import {
  RavenBand,
  RavenSource,
  StringTMap,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-source-explorer',
  styleUrls: ['./source-explorer.component.css'],
  templateUrl: './source-explorer.component.html',
})
export class SourceExplorerComponent implements OnInit {
  bands$: Observable<RavenBand[]>;

  bands: RavenBand[];
  tree: StringTMap<RavenSource>;

  constructor(private changeDetector: ChangeDetectorRef, private store: Store<fromSourceExplorer.SourceExplorerState>) {
    this.bands$ = this.store.select(fromTimeline.getBands);

    this.store.select(fromSourceExplorer.getTreeBySourceId).subscribe(tree => {
      this.tree = tree;
      this.changeDetector.markForCheck();
    });
  }

  ngOnInit() {
    this.store.dispatch(new sourceExplorerActions.FetchInitialSources());
  }

  /**
   * Event. Called when a `close` event is fired from a raven-tree-node.
   */
  onClose(source: RavenSource): void {
    this.bands$.take(1).subscribe(bands => this.bands = bands); // Synchronously get bands from state.
    const { removeBandIds = [], removePointsBandIds = [] } = removeBandsOrPoints(source.id, this.bands);

    this.store.dispatch(new sourceExplorerActions.RemoveBands(source, removeBandIds, removePointsBandIds));
  }

  /**
   * Event. Called when a `collapse` event is fired from a raven-tree-node.
   */
  onCollapse(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SourceExplorerCollapse(source));
  }

  /**
   * Event. Called when an `expand` event is fired from a raven-tree-node.
   */
  onExpand(source: RavenSource): void {
    // Only fetch sources or load content if there are no children (i.e. sources have not been fetched or content has not been loaded yet).
    if (!source.childIds.length) {
      if (source.content.length > 0) {
        this.store.dispatch(new sourceExplorerActions.LoadContent(source, toRavenSources(source.id, false, source.content)));
      } else {
        this.store.dispatch(new sourceExplorerActions.FetchSources(source));
      }
    } else {
      // Otherwise if there are children (i.e. sources have already been fetched or content has already been loaded), then simply expand the source.
      this.store.dispatch(new sourceExplorerActions.SourceExplorerExpand(source));
    }
  }

  /**
   * Event. Called when an `open` event is fired from a raven-tree-node.
   */
  onOpen(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.FetchGraphData(source));
  }

  /**
   * Event. Called when a `select` event is fired from a raven-tree-node.
   */
  onSelect(source: RavenSource): void {
    this.store.dispatch(new sourceExplorerActions.SourceExplorerSelect(source));
  }
}
