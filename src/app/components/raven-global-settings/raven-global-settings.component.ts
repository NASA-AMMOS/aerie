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
  Output,
  ViewContainerRef,
} from '@angular/core';

@Component({
  selector: 'raven-global-settings',
  styleUrls: ['./raven-global-settings.component.css'],
  templateUrl: './raven-global-settings.component.html',
})
export class RavenGlobalSettingsComponent {
  @Input() colorPalette: string[];
  @Input() currentTimeCursor: boolean;
  @Input() dateFormat: string;
  @Input() defaultActivityLayout: number;
  @Input() defaultFillColor: string;
  @Input() defaultIcon: string;
  @Input() defaultLabelFontSize: number;
  @Input() defaultLabelFont: string;
  @Input() defaultResourceColor: string;
  @Input() labelWidth: number;
  @Input() tooltip: boolean;

  @Output() changeDefaultActivityLayout: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeCurrentTimeCursor: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() changeDateFormat: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultFillColor: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultIcon: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultLabelFontSize: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeDefaultLabelFont: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultResourceColor: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeLabelWidth: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeTooltip: EventEmitter<boolean> = new EventEmitter<boolean>();

  constructor(public vcRef: ViewContainerRef) {}

  public onChangeLabelWidth() {
    // emit new labelWidth
    this.changeLabelWidth.emit(this.labelWidth);
  }

  public onChangeDefaultLabelFontSize() {
    // emit new labelWidth
    this.changeDefaultLabelFontSize.emit(this.defaultLabelFontSize);
  }

  public onFillColorPickerClose() {
    // emit new fill color
    this.changeDefaultFillColor.emit(this.defaultFillColor);
  }

  public onResourceColorPickerClose() {
    // emit new default resource color
    this.changeDefaultResourceColor.emit(this.defaultResourceColor);
  }
}
