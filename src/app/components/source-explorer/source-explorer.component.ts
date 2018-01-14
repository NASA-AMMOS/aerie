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

import * as fromSourceExplorer from './../../reducers/source-explorer';
import * as sourceExplorer from './../../actions/source-explorer';

import {
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
  tree$: Observable<RavenSource>;

  constructor(private store: Store<fromSourceExplorer.SourceExplorerState>) {
    this.tree$ = this.store
      .select(fromSourceExplorer.getTreeBySourceId)
      .map(treeBySourceId => this.treeFromTreeBySourceId(treeBySourceId));
  }

  ngOnInit() {
    this.store.dispatch(new sourceExplorer.FetchInitialSources());
  }

  /**
   * Event. Called when `collapse-falcon-source-explorer-tree-node` event is fired from falcon-source-explorer-tree.
   */
  onCollapse(e: FalconSourceExplorerTreeEvent) {
    this.store.dispatch(new sourceExplorer.SourceExplorerCollapse(e.detail.data));
  }

  /**
   * Event. Called when `expand-falcon-source-explorer-tree-node` event is fired from falcon-source-explorer-tree.
   */
  onExpand(e: FalconSourceExplorerTreeEvent) {
    if (e.detail.data.children && e.detail.data.children.length > 0) {
      this.store.dispatch(new sourceExplorer.SourceExplorerExpand(e.detail.data));
    } else {
      this.store.dispatch(new sourceExplorer.SourceExplorerExpandWithFetchSources(e.detail.data));
    }
  }

  /**
   * Helper. Convert a treeBySourceId to a tree so it can be consumed by falcon-source-explorer-tree.
   */
  treeFromTreeBySourceId(treeBySourceId: StringTMap<RavenSource>): RavenSource {
    const rootNode: RavenSource = { ...treeBySourceId['0'] }; // Return a new rootNode object so falcon-source-explorer-tree properly updates.

    (function dfs(node: RavenSource): void {
      if (node && node.childIds.length > 0) {
        node.children = node.childIds.map(id => ({ ...treeBySourceId[id] })); // Build a nodes children based on it's childIds.
        node.children.forEach((child: RavenSource) => dfs(child)); // Now recurse via depth-first-search to build each child's children (ha).
      }
    }(rootNode));

    return rootNode;
  }
}
