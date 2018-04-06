import { Component, EventEmitter, Input, Output, ViewContainerRef } from '@angular/core';
import { Cmyk } from 'ngx-color-picker';

@Component({
  selector: 'raven-global-settings',
  styleUrls: ['./raven-global-settings.component.css'],
  templateUrl: './raven-global-settings.component.html',
})
export class RavenGlobalSettingsComponent {

  // resourceColor = '#2889e9';
  @Input() dateFormat: string;
  @Input() labelWidth: number;
  @Input() tooltip: boolean;
  @Input() currentTimeCursor: boolean;
  @Input() labelFontSize: number;
  @Input() labelFontStyle: string;
  @Input() resourceColor: string;
  @Input() colorPalette: string[];

  @Output() changeDateFormat: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeResourceColor: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeLabelWidth: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeLabelFontSize: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeLabelFontStyle: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeTooltip: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() changeCurrentTimeCursor: EventEmitter<boolean> = new EventEmitter<boolean>();


  public cmykColor: Cmyk = new Cmyk(0, 0, 0, 0);

  constructor(public vcRef: ViewContainerRef) {}

  public onEventLog(event: string, data: any): void {
    console.log('log: ' + event, data);
    console.log('color:' + this.resourceColor);
  }

  public onColorPickerClose() {
    this.changeResourceColor.emit(this.resourceColor);
  }

  public onChangeLabelWidth() {
    // emit new labelWidth
    this.changeLabelWidth.emit(this.labelWidth);
  }

  public onChangeLabelFontSize(size: number) {
    console.log('label font size changed' + size);
    console.log('2. label font size changed' + this.labelFontSize);
    // emit new labelWidth
    this.changeLabelFontSize.emit(this.labelFontSize);
  }
}
