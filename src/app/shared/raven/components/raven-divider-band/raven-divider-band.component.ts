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
  OnDestroy,
  OnInit,
  Output,
  SimpleChanges,
} from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'raven-divider-band',
  styles: [``],
  template: ``,
})
export class RavenDividerBandComponent implements OnChanges, OnDestroy, OnInit {
  @Input() borderWidth: number;
  @Input() color: number[];
  @Input() ctlTimeAxis: any;
  @Input() ctlViewTimeAxis: any;
  @Input() height: number;
  @Input() id: string;
  @Input() label: string;
  @Input() labelColor: number[];
  @Input() labelFont: string;
  @Input() labelFontSize: number;
  @Input() name: string;

  @Output() addSubBand: EventEmitter<any> = new EventEmitter<any>();
  @Output() removeSubBand: EventEmitter<string> = new EventEmitter<string>();
  @Output() updateSubBand: EventEmitter<any> = new EventEmitter<any>();

  ngOnChanges(changes: SimpleChanges) {
    // Border Width.
    if (changes.borderWidth && !changes.borderWidth.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'painter', prop: 'borderWidth', value: this.borderWidth });
    }

    // Color.
    if (changes.color && !changes.color.firstChange) {
      // TODO.
    }

    // Label.
    if (changes.label && !changes.label.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'label', value: this.label });
    }

    // Label Color.
    if (changes.labelColor && !changes.labelColor.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, prop: 'labelColor', value: this.labelColor });
    }

    // Label Font Size.
    if (changes.labelFontSize && !changes.labelFontSize.firstChange) {
      this.updateSubBand.emit({ subBandId: this.id, subObject: 'decorator', prop: 'labelFontSize', value: this.labelFontSize });
    }
  }

  ngOnInit() {
    // Create Divider Band.
    // Note how we use the CTL StateBand since CTL has no DividerBand.
    const ctlDividerBand = new (window as any).StateBand({
      borderWidth: this.borderWidth,
      height: this.height,
      id: this.id,
      intervals: [],
      label: this.label,
      labelColor: this.labelColor,
      labelFont: this.labelFont,
      labelFontSize: this.labelFontSize,
      name: this.name,
      timeAxis: this.ctlTimeAxis,
      viewTimeAxis: this.ctlViewTimeAxis,
    });

    // Send the newly created divider band to the parent composite band so it can be added.
    // All subsequent updates should be made to the parent composite sub-band via events.
    this.addSubBand.emit(ctlDividerBand);
  }

  ngOnDestroy() {
    this.removeSubBand.emit(this.id);
  }
}
