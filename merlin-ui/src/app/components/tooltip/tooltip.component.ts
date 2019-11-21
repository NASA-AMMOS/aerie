import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-tooltip',
  styleUrls: ['./tooltip.component.css'],
  template: '',
})
export class TooltipComponent {}
