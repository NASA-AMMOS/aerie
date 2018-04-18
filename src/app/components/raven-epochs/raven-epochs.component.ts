import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
} from '@angular/core';

import { SelectionModel } from '@angular/cdk/collections';
import { OnInit } from '@angular/core/src/metadata/lifecycle_hooks';
import {
  MatSort,
  Sort,
} from '@angular/material';

import { RavenEpoch } from '../../shared/models';
@Component({
  selector: 'raven-epochs',
  styleUrls: ['./raven-epochs.component.css'],
  templateUrl: './raven-epochs.component.html',
})
export class RavenEpochsComponent implements OnInit {
  @Input() dayCode: string;
  @Input() earthSecToEpochSec: number;
  @Input() epochs: RavenEpoch[];
  @Input() inUseEpoch: RavenEpoch | null;

  @Output() changeDayCode: EventEmitter<string> = new EventEmitter<string>();
  @Output() changeEarthSecToEpochSec: EventEmitter<number> = new EventEmitter<number>();
  @Output() importEpochs: EventEmitter<RavenEpoch[]> = new EventEmitter<RavenEpoch[]>();
  @Output() selectEpoch: EventEmitter<RavenEpoch> = new EventEmitter<RavenEpoch>();

  selection = new SelectionModel<RavenEpoch>(true, []);

  displayedColumns = ['select', 'name', 'value'];

  originalEpochs: RavenEpoch[];

  @ViewChild(MatSort) sort: MatSort;

  ngOnInit() {
    this.originalEpochs = this.epochs.slice(0);
  }

  addEpochs(content: any): void {
    const newEpochs: RavenEpoch[] = JSON.parse(content);
    this.importEpochs.emit(newEpochs);
  }

  applyFilter(filterValue: string) {
    filterValue = filterValue.trim();
    const copy = this.originalEpochs.slice(0);

    // match either name or value
    this.epochs = copy.filter(epoch =>
      (epoch.name.indexOf(filterValue) > -1) || (epoch.value.indexOf(filterValue) > -1),
    );
  }

  getContent($event: any): void {
    this.readFile($event.target);
  }

  readFile(inputValue: any): void {
    const file: File = inputValue.files[0];
    const reader: FileReader = new FileReader();
    reader.onloadend = (e) => {
      this.addEpochs(reader.result);
    };

    reader.readAsText(file);
  }

  sortData(sort: Sort) {
    const data = this.epochs.slice();
    if (!sort.active || sort.direction === '') {
      this.epochs = data;
      return;
    }

    this.epochs = data.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'name': return compare(a.name, b.name, isAsc);
        case 'value': return compare(a.value, b.value, isAsc);
        default: return 0;
      }
    });

    function compare(a: string, b: string, isAsc: boolean) {
      return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
    }
  }
}

