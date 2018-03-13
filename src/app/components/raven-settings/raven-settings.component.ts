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
  RavenCompositeBand,
  RavenSettingsUpdate,
  RavenSubBand,
  StringTMap,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-settings',
  styleUrls: ['./raven-settings.component.css'],
  templateUrl: './raven-settings.component.html',
})
export class RavenSettingsComponent implements OnChanges {
  @Input() bandsById: StringTMap<RavenCompositeBand>;
  @Input() labelWidth: number;
  @Input() selectedBandId: string;

  @Output() deleteBand: EventEmitter<RavenSubBand> = new EventEmitter<RavenSubBand>();
  @Output() updateBand: EventEmitter<RavenSettingsUpdate> = new EventEmitter<RavenSettingsUpdate>();
  @Output() updateSubBand: EventEmitter<RavenSettingsUpdate> = new EventEmitter<RavenSettingsUpdate>();
  @Output() updateTimeline: EventEmitter<RavenSettingsUpdate> = new EventEmitter<RavenSettingsUpdate>();

  selectedSubBandId: string;

  ngOnChanges(changes: SimpleChanges) {
    const selectedBand = this.bandsById[this.selectedBandId];

    if (changes.selectedBandId && selectedBand) {
      // Set the selected sub-band to be the first band in the selected band's sub-bands.
      this.selectedSubBandId = selectedBand.subBands[0].id;
    } else if (changes.bandsById && selectedBand) {
      // Every time our bands change, make sure the selected sub-band is still there.
      // If it was removed, reset it to be the first band in the selected band's sub-bands.
      const subBandIds = selectedBand.subBands.map(subBand => subBand.id);

      if (!subBandIds.includes(this.selectedSubBandId)) {
        this.selectedSubBandId = selectedBand.subBands[0].id;
      }
    }
  }
}
