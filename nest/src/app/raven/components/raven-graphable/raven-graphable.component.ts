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
import { RavenGraphableSource, RavenSourceActionEvent } from '../../models';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-graphable',
  styleUrls: ['./raven-graphable.component.css'],
  templateUrl: './raven-graphable.component.html',
})
export class RavenGraphableComponent implements OnChanges {
  @Input()
  source: RavenGraphableSource;

  @Output()
  action: EventEmitter<RavenSourceActionEvent> = new EventEmitter<
    RavenSourceActionEvent
  >();

  @Output()
  close: EventEmitter<RavenGraphableSource> = new EventEmitter<
    RavenGraphableSource
  >();

  @Output()
  open: EventEmitter<RavenGraphableSource> = new EventEmitter<
    RavenGraphableSource
  >();

  menu: any;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.source) {
      if (this.source.opened) {
        this.menu = [
          ...this.source.actions,
          {
            event: 'graph-again',
            name: 'Graph Again',
          },
        ];
      } else {
        this.menu = this.source.actions;
      }
    }
  }
}
