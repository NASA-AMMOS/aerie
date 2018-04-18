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
  @Input() defaultResourceColor: string;
  @Input() defaultFillColor: string;
  @Input() labelFontSize: number;
  @Input() labelFontStyle: string;
  @Input() labelWidth: number;
  @Input() tooltip: boolean;

  @Output() changeDefaultActivityLayout: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeCurrentTimeCursor: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() changeDateFormat: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultFillColor: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeDefaultResourceColor: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeLabelWidth: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeLabelFontSize: EventEmitter<number> = new EventEmitter<number>();
  @Output() changeLabelFontStyle: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeTooltip: EventEmitter<boolean> = new EventEmitter<boolean>();

  constructor(public vcRef: ViewContainerRef) {}

  public onChangeLabelWidth() {
    // emit new labelWidth
    this.changeLabelWidth.emit(this.labelWidth);
  }

  public onChangeLabelFontSize(size: number) {
    // emit new labelWidth
    this.changeLabelFontSize.emit(this.labelFontSize);
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
