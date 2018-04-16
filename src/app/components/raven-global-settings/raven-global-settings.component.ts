import { Component, EventEmitter, Input, Output, ViewContainerRef } from '@angular/core';
import { Cmyk } from 'ngx-color-picker';

@Component({
  selector: 'raven-global-settings',
  styleUrls: ['./raven-global-settings.component.css'],
  templateUrl: './raven-global-settings.component.html',
})
export class RavenGlobalSettingsComponent {

  @Input() colorPalette: string[];
  @Input() currentTimeCursor: boolean;
  @Input() dateFormat: string;
  @Input() defaultResourceColor: string;
  @Input() defaultFillColor: string;
  @Input() labelFontSize: number;
  @Input() labelFontStyle: string;
  @Input() labelWidth: number;
  @Input() tooltip: boolean;

  @Output() changeCurrentTimeCursor: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() changeDateFormat: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultFillColor: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultResourceColor: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeLabelWidth: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeLabelFontSize: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeLabelFontStyle: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeTooltip: EventEmitter<boolean> = new EventEmitter<boolean>();


  public cmykColor: Cmyk = new Cmyk(0, 0, 0, 0);

  constructor(public vcRef: ViewContainerRef) {}

  public onChangeLabelWidth() {
    // emit new labelWidth
    this.changeLabelWidth.emit(this.labelWidth);
  }

  public onChangeLabelFontSize(size: number) {
    // emit new labelWidth
    this.changeLabelFontSize.emit(this.labelFontSize);
  }

  public onEventLog(event: string, data: any): void {
    console.log('log: ' + event, data);
    console.log('color:' + this.defaultResourceColor);
  }

  public onFillColorPickerClose() {
    this.changeDefaultFillColor.emit(this.defaultFillColor);
  }

  public onResourceColorPickerClose() {
    this.changeDefaultResourceColor.emit(this.defaultResourceColor);
  }
}
