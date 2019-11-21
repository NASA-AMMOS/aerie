import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { ContextMenu } from '../../classes';
import { CPlan } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-plans-table',
  styleUrls: ['./plans-table.component.css'],
  templateUrl: './plans-table.component.html',
})
export class PlansTableComponent extends ContextMenu {
  @Input()
  plans: CPlan[] = [];

  @Output()
  deletePlan: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  openPlan: EventEmitter<string> = new EventEmitter<string>();

  displayedColumns: string[] = [
    'name',
    'adaptationId',
    'startTimestamp',
    'endTimestamp',
  ];
}
