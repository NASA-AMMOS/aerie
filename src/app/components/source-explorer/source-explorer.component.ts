/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, ChangeDetectionStrategy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/take';

import * as fromSourceExplorer from './../../reducers/source-explorer';
import * as fromTimeline from './../../reducers/timeline';

import * as sourceExplorerActions from './../../actions/source-explorer';

import { toRavenSources } from './../../util/source';
import { removeBandsOrPoints } from './../../util/bands';

import {
  RavenBand,
  RavenSource,
  StringTMap,
} from './../../models';

interface FalconSourceExplorerTreeNode extends HTMLElement {
  data: RavenSource;
}

interface FalconSourceExplorerTreeEvent extends Event {
  detail: FalconSourceExplorerTreeNode;
}

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-source-explorer',
  styleUrls: ['./source-explorer.component.css'],
  templateUrl: './source-explorer.component.html',
})
export class SourceExplorerComponent implements OnInit {
  bands: RavenBand[];
  bands$: Observable<RavenBand[]>;
  tree$: Observable<RavenSource>;

  constructor(private store: Store<fromSourceExplorer.SourceExplorerState>) {
    this.bands$ = this.store.select(fromTimeline.getBands);

    this.tree$ = this.store
      .select(fromSourceExplorer.getTreeBySourceId)
      .map(treeBySourceId => this.treeFromTreeBySourceId(treeBySourceId));
  }

  ngOnInit() {
    this.store.dispatch(new sourceExplorerActions.FetchInitialSources());
  }

  /**
   * Event. Called when `collapse-falcon-source-explorer-tree-node` event is fired from falcon-source-explorer-tree.
   */
  onCollapse(e: FalconSourceExplorerTreeEvent) {
    const source = e.detail.data;
    this.store.dispatch(new sourceExplorerActions.SourceExplorerCollapse(source));
  }

  /**
   * Event. Called when `expand-falcon-source-explorer-tree-node` event is fired from falcon-source-explorer-tree.
   */
  onExpand(e: FalconSourceExplorerTreeEvent) {
    const source = e.detail.data;

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
   * Event. Called when `open-falcon-source-explorer-tree-node` event is fired from falcon-source-explorer-tree.
   */
  onOpen(e: FalconSourceExplorerTreeEvent) {
    const source = e.detail.data;
    this.store.dispatch(new sourceExplorerActions.FetchGraphData(source));
  }

  /**
   * Event. Called when `close-falcon-source-explorer-tree-node` event is fired from falcon-source-explorer-tree.
   */
  onClose(e: FalconSourceExplorerTreeEvent) {
    const source = e.detail.data;

    this.bands$.take(1).subscribe(bands => this.bands = bands); // Synchronously get bands from state.
    const { removeBandIds = [], removePointsBandIds = [] } = removeBandsOrPoints(source.id, this.bands);

    this.store.dispatch(new sourceExplorerActions.RemoveBands(source, removeBandIds, removePointsBandIds));
  }

  /**
   * Helper. Convert a treeBySourceId to a tree so it can be consumed by falcon-source-explorer-tree.
   */
  treeFromTreeBySourceId(treeBySourceId: StringTMap<RavenSource>): RavenSource {
    const rootNode: RavenSource = { ...treeBySourceId['0'] }; // Return a new rootNode object so falcon-source-explorer-tree properly updates.

    (function dfs(node: RavenSource): void {
      if (node && node.childIds.length > 0) {
        node.children = node.childIds.map(id => ({ ...treeBySourceId[id] })); // Build a nodes children based on it's childIds.
        node.children.forEach((child: RavenSource) => dfs(child)); // Now recurse via depth-first-search to build each child's children.
      }
    }(rootNode));

    return rootNode;
  }
}
