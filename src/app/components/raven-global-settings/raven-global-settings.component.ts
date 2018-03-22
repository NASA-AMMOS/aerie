import { Component, EventEmitter, Input, Output, ViewContainerRef } from '@angular/core';
import { Cmyk } from 'ngx-color-picker';

@Component({
  selector: 'raven-global-settings',
  styleUrls: ['./raven-global-settings.component.css'],
  templateUrl: './raven-global-settings.component.html',
})
export class RavenGlobalSettingsComponent {

  // resourceColor = '#2889e9';
  @Input() labelWidth: number;
  @Input() tooltip: boolean;
  @Input() currentTimeCursor: boolean;
  @Input() labelFontSize: number;
  @Input() labelFontStyle: string;
  @Input() resourceColor: string;

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

  public onChangeColor(color: string): void {
    console.log('color changed color:' + color);

    // emit new color
    this.changeResourceColor.emit(color);
  }

  public onLabelWidthChange() {
    console.log('label width changed' + this.labelWidth);

    // emit new labelWidth
    this.changeLabelWidth.emit(this.labelWidth);
  }

  public onLabelFontSizeChange() {
    console.log('label width changed' + this.labelWidth);

    // emit new labelWidth
    this.changeLabelFontSize.emit(this.labelFontSize);
  }

  public onLabelFontStyleChange() {
    console.log('label width changed' + this.labelWidth);

    // emit new labelWidth
    this.changeLabelFontStyle.emit(this.labelFontStyle);
  }

  public onTooltipChange() {
    console.log('tooltip changed' + this.tooltip);

    // emit new labelWidth
    this.changeTooltip.emit(this.tooltip);
  }

  public onCurrentTimeCursorChange() {
    console.log('current time cursor changed' + this.currentTimeCursor);

    // emit new labelWidth
    this.changeCurrentTimeCursor.emit(this.currentTimeCursor);
  }
}
