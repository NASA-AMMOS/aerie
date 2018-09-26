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
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';

import {
  RavenApplyLayoutUpdate,
  RavenPin,
  RavenSource,
  RavenState,
  StringTMap,
} from '../../models';

import { FormControl } from '@angular/forms';
import { MatSelect } from '@angular/material';
import { keyBy } from 'lodash';
import { getAllSourcesByKinds } from '../../util/source';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-layout-apply',
  styleUrls: ['./raven-layout-apply.component.css'],
  templateUrl: './raven-layout-apply.component.html',
})
export class RavenLayoutApplyComponent implements OnChanges {
  @Input()
  currentState: RavenState | null;

  @Input()
  currentStateId: string;

  @Input()
  treeBySourceId: StringTMap<RavenSource>;

  @Output()
  applyLayout: EventEmitter<RavenApplyLayoutUpdate> = new EventEmitter<
    RavenApplyLayoutUpdate
  >();

  @Output()
  applyLayoutWithPins: EventEmitter<RavenApplyLayoutUpdate> = new EventEmitter<
    RavenApplyLayoutUpdate
  >();

  @Output()
  applyState: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  applyPins = '0';
  applyPinsByName: StringTMap<RavenPin>;
  filteredSources: RavenSource[];
  originalPinsByName: StringTMap<RavenPin>;
  sourcesFormControl = new FormControl();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.currentState && this.currentState && this.currentState.pins) {
      this.applyPinsByName = {};
      this.originalPinsByName = keyBy(
        this.currentState.pins,
        'name',
      ) as StringTMap<RavenPin>;
    }

    if (changes.treeBySourceId) {
      this.filteredSources = this.filterSources(this.treeBySourceId);
    }
  }

  /**
   * Returns true if we can apply a layout.
   * False otherwise.
   *
   * We can apply a layout if:
   * 1. The select form is valid and,
   * 2. We are applying new sources to the saved state pins and they are all accounted for.
   */
  canApplyLayout(): boolean {
    let pinsValid = true;

    if (this.currentState) {
      this.currentState.pins.forEach(pin => {
        pinsValid = pinsValid && this.applyPinsByName[pin.name] !== undefined;
      });
    }

    return (
      (this.sourcesFormControl.valid && pinsValid) || this.applyPins === '0'
    );
  }

  /**
   * Event. Called when the `apply` button is clicked.
   */
  onApply(): void {
    if (this.canApplyLayout()) {
      const update: RavenApplyLayoutUpdate = {
        pins: this.applyPinsByName,
        targetSourceIds: this.sourcesFormControl.value
          ? this.sourcesFormControl.value.map((value: RavenSource) => value.id)
          : [],
      };

      if (this.applyPins === '0') {
        // Apply saved state pins.
        this.applyState.emit(this.treeBySourceId[this.currentStateId]);
      } else if (this.applyPins === '1') {
        // Use custom sources for pins.
        this.applyLayoutWithPins.emit(update);
      } else {
        // Apply layout with generic target ids.
        this.applyLayout.emit(update);
        this.sourcesFormControl = new FormControl(); // Reset the form.
      }
    }
  }

  /**
   * Helper that filters sources based on the given filter criteria.
   *
   * Include source in the apply layout selection if:
   * 1. It's not a state.
   * 2. It's an fs_file with an expanded fs_dir.
   * 3. It's an fs_dir with at least one fs_graphable child.
   * 4. It's in the source tree and is in the original pins.
   */
  filterSources(tree: StringTMap<RavenSource>): RavenSource[] {
    // If a source is visible from our pins then include it in the list.
    const pins = this.currentState ? this.currentState.pins : [];
    const pinSources: RavenSource[] = pins.reduce(
      (sources: RavenSource[], pin) => {
        const source = tree[pin.sourceId];
        if (source) {
          sources.push(source);
        }
        return sources;
      },
      [],
    );

    return pinSources.concat(
      getAllSourcesByKinds(tree, '/', [
        'fs_dir',
        'fs_file',
        'fs_graphable',
      ]).filter(
        (source: RavenSource) =>
          ((tree[source.id].subKind !== 'file_state' &&
            (tree[source.parentId].kind === 'fs_dir' &&
              tree[source.parentId].expanded &&
              tree[source.id].kind === 'fs_file')) ||
            (tree[source.id].kind === 'fs_dir' &&
              tree[source.id].childIds.reduce(
                (include, childId) =>
                  include || tree[childId].kind === 'fs_graphable',
                false,
              ))) &&
          !pinSources.includes(tree[source.id]),
      ),
    );
  }

  /**
   * Event. Called when a mat-selection changes in the pinning table.
   */
  onSelectionChange(select: MatSelect, pin: RavenPin) {
    this.applyPinsByName[pin.name] = {
      ...pin,
      sourceId: select.value,
    };
  }
}
