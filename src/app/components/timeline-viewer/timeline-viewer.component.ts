import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timeline-viewer',
  styles: [],
  template: `
    <div class="mui-panel">
      <falcon-timeline [bands]="bands" label-width="200"></falcon-timeline>
    </div>
  `,
})
export class TimelineViewerComponent {
  bands: any;

  constructor() {
    this.bands = (window as any).bands; // TODO. Remove. Just for demo purposes.
  }
}
