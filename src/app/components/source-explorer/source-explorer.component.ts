import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-source-explorer',
  styleUrls: ['./source-explorer.component.css'],
  template: `
    <div class="pad">
      <mat-card>
        Source Explorer
      </mat-card>
    </div>
  `,
})
export class SourceExplorerComponent {}
