import { Component, ViewContainerRef } from '@angular/core';

import { Cmyk, ColorPickerService } from 'ngx-color-picker';

@Component({
  selector: 'raven-global-settings',
  styleUrls: ['./raven-global-settings.component.css'],
  templateUrl: './raven-global-settings.component.html',
})
export class RavenGlobalSettingsComponent {

  color = '#2889e9';

  colorPalette = ['#2889e9',
    '#e920e9',
    '#fff500',
    'rgb(236,64,64)',
    'rgba(45,208,45,1)',
    '#1973c0',
    '#f200bd',
    '#a8ff00',
    '#278ce2',
    '#0a6211',
    '#f2ff00'];

  public cmykColor: Cmyk = new Cmyk(0, 0, 0, 0);
  constructor(public vcRef: ViewContainerRef, private cpService: ColorPickerService) { }

  public onEventLog(event: string, data: any): void {
    console.log(event, data);
  }

  public onChangeColor(color: string): Cmyk {
    const hsva = this.cpService.stringToHsva(color);

    const rgba = this.cpService.hsvaToRgba(hsva);

    return this.cpService.rgbaToCmyk(rgba);
  }

  public onChangeColorHex8(color: string): string {
    const hsva = this.cpService.stringToHsva(color, true);

    return this.cpService.outputFormat(hsva, 'rgba', '');
  }
}
