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

import { OnChanges, SimpleChanges } from '@angular/core/src/metadata/lifecycle_hooks';

// import { MatSliderChange } from '@angular/material';

import {
  RavenBand,
  RavenSettingsUpdate,
} from './../../shared/models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-settings',
  styleUrls: ['./raven-settings.component.css'],
  templateUrl: './raven-settings.component.html',
})
export class RavenSettingsComponent implements OnChanges {
  @Input() bands: RavenBand[];
  @Input() selectedBandId: string;

  @Output() updateBand: EventEmitter<RavenSettingsUpdate> = new EventEmitter<RavenSettingsUpdate>();

  selectedBandIndex: number | null;

  ngOnChanges(changes: SimpleChanges) {
    if (changes.selectedBandId) {
      this.selectedBandIndex = null;

      this.bands.forEach((band, index) => {
        if (band.id === this.selectedBandId) { this.selectedBandIndex = index; }
      });
    }
  }
}
