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
  Output,
} from '@angular/core';

import {
  RavenCompositeBand,
  RavenSubBand,
  RavenUpdate,
  StringTMap,
} from './../../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-settings-bands',
  styleUrls: ['./raven-settings-bands.component.css'],
  templateUrl: './raven-settings-bands.component.html',
})
export class RavenSettingsBandsComponent {
  @Input() bandsById: StringTMap<RavenCompositeBand>;
  @Input() selectedBandId: string;
  @Input() selectedSubBandId: string;

  @Output() deleteSubBand: EventEmitter<RavenSubBand> = new EventEmitter<RavenSubBand>();
  @Output() updateBand: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();
  @Output() updateBandAndSubBand: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();
  @Output() updateSubBand: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();
  @Output() updateTimeline: EventEmitter<RavenUpdate> = new EventEmitter<RavenUpdate>();

  /**
   * Returns true if the selected band contains more than one resource sub-band. False otherwise.
   */
  containsMultipleResourceBands(): boolean {
    const subBands = this.bandsById[this.selectedBandId].subBands;
    const resourceCount = subBands.reduce((count, subBand) => subBand.type === 'resource' ? count + 1 : count, 0);
    return resourceCount > 1;
  }

  /**
   * trackBy for subBands.
   */
  trackByFn(index: number, item: RavenSubBand) {
    return item.id;
  }
}
