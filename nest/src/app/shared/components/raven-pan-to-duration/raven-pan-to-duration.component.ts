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
  Output,
} from '@angular/core';

import { FormControl, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { RavenTimeRange } from '../../models';
import { fromDHMString, utc } from '../../util/time';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-pan-to-duration',
  styleUrls: ['./raven-pan-to-duration.component.css'],
  templateUrl: './raven-pan-to-duration.component.html',
})
export class RavenPanToDurationComponent {
  @Output()
  panTo: EventEmitter<RavenTimeRange> = new EventEmitter<RavenTimeRange>();

  durations = [
    '10s',
    '20s',
    '30s',
    '40s',
    '50s',
    '60s',
    '1m',
    '10m',
    '20m',
    '30m',
    '40m',
    '50m',
    '60m',
    '1h',
    '2h',
    '3h',
    '4h',
    '5h',
    '6h',
    '7h',
    '8h',
    '9h',
    '10h',
    '11h',
    '12h',
    '1d',
    '2d',
    '3d',
    '4d',
    '5d',
    '6d',
    '7d',
    '8d',
    '9d',
    '10d',
    '11d',
    '12d',
  ];

  filteredDurations: Observable<string[]>;

  panToControl: FormControl = new FormControl('', [
    Validators.required,
    Validators.pattern(/(\d\d\d\d)-(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?/),
  ]);

  panDurationControl: FormControl = new FormControl('', [
    Validators.required,
    Validators.pattern(
      /^((\d+)[d,D])?((\d+)[h,H])?((\d+)[m,M])?((\d+)[s,S])?((\d+)ms)?$/,
    ),
  ]);

  constructor() {
    this.filteredDurations = this.panDurationControl.valueChanges.pipe(
      startWith(''),
      map(duration =>
        duration ? this.filterDurations(duration) : [...this.durations],
      ),
    );
  }

  /**
   * Called when the pan search button is clicked.
   */
  onPan() {
    if (this.panDurationControl.valid && this.panToControl.valid) {
      const start: number = utc(this.panToControl.value);
      const duration: number = fromDHMString(this.panDurationControl.value);
      this.panTo.emit({ end: start + duration, start });
    }
  }

  /**
   * Helper that filters the durations array based on a duration.
   */
  filterDurations(duration: string) {
    return this.durations.filter(
      d => d.toLowerCase().indexOf(duration.toLowerCase()) === 0,
    );
  }
}
