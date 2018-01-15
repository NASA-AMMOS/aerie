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

import * as fromTimeline from '../../reducers/timeline';

import {
  RavenBand,
} from './../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timeline',
  styleUrls: ['./timeline.component.css'],
  templateUrl: './timeline.component.html',
})
export class TimelineComponent implements OnInit {
  bands$: Observable<RavenBand[]>;
  labelWidth$: Observable<number>;

  constructor(private store: Store<fromTimeline.TimelineState>) {
    this.bands$ = this.store.select(fromTimeline.getBands);
    this.labelWidth$ = this.store.select(fromTimeline.getLabelWidth);
  }

  ngOnInit() {
  }
}
