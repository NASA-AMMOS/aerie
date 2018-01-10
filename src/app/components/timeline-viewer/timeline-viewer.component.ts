import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-timeline-viewer',
  styleUrls: ['./timeline-viewer.component.css'],
  template: `
    <div class="pad">
      <mat-card>
        Simple card
      </mat-card>
    </div>
  `,
})
export class TimelineViewerComponent {}
