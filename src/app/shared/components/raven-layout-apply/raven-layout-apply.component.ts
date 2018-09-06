/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';

import { FormControl } from '@angular/forms';
import { RavenSource, StringTMap } from '../../models';
import { getAllSourcesByKinds } from '../../util/source';

@Component({
  selector: 'raven-layout-apply',
  styleUrls: ['./raven-layout-apply.component.css'],
  templateUrl: './raven-layout-apply.component.html',
})
export class RavenLayoutApplyComponent implements OnChanges {
  @Input()
  currentStateId: string;

  @Input()
  treeBySourceId: StringTMap<RavenSource>;

  @Output()
  applyLayout: EventEmitter<string[]> = new EventEmitter<string[]>();

  filteredSources: RavenSource[];
  sourcesFormControl = new FormControl();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.treeBySourceId) {
      const tree = this.treeBySourceId;

      this.filteredSources = getAllSourcesByKinds(tree, '/', [
        'fs_dir',
        'fs_file',
        'fs_graphable',
      ]).filter(
        // Include source in the apply layout selection if:
        // 1. It's not a state.
        // 2. It's an fs_file with an expanded fs_dir.
        // 3. It's an fs_dir with at least one fs_graphable child.
        (source: RavenSource) =>
          (tree[source.id].subKind !== 'file_state' &&
            (tree[source.parentId].kind === 'fs_dir' &&
              tree[source.parentId].expanded &&
              tree[source.id].kind === 'fs_file')) ||
          (tree[source.id].kind === 'fs_dir' &&
            tree[source.id].childIds.reduce(
              (include, childId) =>
                include || tree[childId].kind === 'fs_graphable',
              false,
            )),
      );
    }
  }

  /**
   * Event. Called when the `apply` button is clicked.
   */
  onApply() {
    this.applyLayout.emit(
      this.sourcesFormControl.value.map((value: RavenSource) => value.id),
    );
    this.sourcesFormControl.setErrors({ valid: false });
  }
}
