import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import { CActivityType } from '../../types';

@Component({
  selector: 'app-activity-type-list',
  styles: [''],
  templateUrl: './activity-type-list.component.html',
})
export class ActivityTypeListComponent implements OnChanges {
  @Input()
  activityTypes: CActivityType[] = [];

  @Output()
  selectActivityType: EventEmitter<CActivityType> = new EventEmitter<
    CActivityType
  >();

  filteredActivityTypes: CActivityType[] = [];
  searchText = '';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.activityTypes) {
      this.filterActivityTypes(this.searchText);
    }
  }

  filterActivityTypes(text: string): void {
    this.filteredActivityTypes = this.activityTypes.filter(activityType =>
      activityType.name.toLowerCase().includes(text.toLowerCase()),
    );
    this.searchText = text;
  }

  onActivityTypeSelect(activityType: CActivityType): void {
    this.selectActivityType.emit(activityType);
  }
}
