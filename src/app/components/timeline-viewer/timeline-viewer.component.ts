import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timeline-viewer',
  styles: [],
  template: `
    <div class="mui-panel">
      Hi
    </div>
  `,
})
export class TimelineViewerComponent {
  bands: any;

  constructor() {
  }
}
