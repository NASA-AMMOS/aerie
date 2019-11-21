import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { ContextMenu } from '../../classes';
import { CAdaptation } from '../../types';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-adaptations-table',
  styleUrls: ['./adaptations-table.component.css'],
  templateUrl: './adaptations-table.component.html',
})
export class AdaptationsTableComponent extends ContextMenu {
  @Input()
  adaptations: CAdaptation[] = [];

  @Output()
  createPlan: EventEmitter<string> = new EventEmitter<string>();

  @Output()
  deleteAdaptation: EventEmitter<string> = new EventEmitter<string>();

  displayedColumns: string[] = ['id', 'name', 'version', 'mission', 'owner'];
}
