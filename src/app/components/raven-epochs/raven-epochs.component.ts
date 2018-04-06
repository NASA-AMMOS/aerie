import {
  Component,
  EventEmitter,
  Input,
  Output,
  ViewChild,
} from '@angular/core';

import { RavenEpoch } from '../../shared/models';

import { MatSort, Sort } from '@angular/material';

import { SelectionModel } from '@angular/cdk/collections';

import { OnInit } from '@angular/core/src/metadata/lifecycle_hooks';

@Component({
  selector: 'raven-epochs',
  styleUrls: ['./raven-epochs.component.css'],
  templateUrl: './raven-epochs.component.html',
})
export class RavenEpochsComponent implements OnInit {
  @Input() currentSelectedEpoch: RavenEpoch | null;
  @Input() epochs: RavenEpoch[];
  @Input() inUseEpoch: RavenEpoch | null;

  @Output() selectEpoch: EventEmitter<RavenEpoch> = new EventEmitter<RavenEpoch>();
  @Output() importEpochs: EventEmitter<RavenEpoch[]> = new EventEmitter<RavenEpoch[]>();

  selection = new SelectionModel<RavenEpoch>(true, []);

  displayedColumns = ['select', 'name', 'value'];

  originalEpochs: RavenEpoch[];

  @ViewChild(MatSort) sort: MatSort;

  ngOnInit() {
    this.originalEpochs = this.epochs.slice(0);
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

  applyFilter(filterValue: string) {
    filterValue = filterValue.trim(); // Remove whitespace
    filterValue = filterValue.toLowerCase(); // MatTableDataSource defaults to lowercase matches
    const copy = this.originalEpochs.slice(0);
    this.epochs = copy.filter(epoch =>
      (epoch.name.indexOf(filterValue) > -1) || (epoch.value.indexOf(filterValue) > -1),
    );
  }

  onSelect(epoch: RavenEpoch): void {
    console.log('in onSelect epoch:' + JSON.stringify(epoch));
    this.selectEpoch.emit(epoch);
  }

  getContent($event: any): void {
    this.readFile($event.target);
  }

  addEpochs(content: any): void {
    const newEpochs: RavenEpoch[] = JSON.parse(content);
    console.log('newEpochs: ' + JSON.stringify(newEpochs));
    this.importEpochs.emit(newEpochs);
  }

  readFile(inputValue: any): void {
    const file: File = inputValue.files[0];
    const reader: FileReader = new FileReader();
    reader.onloadend = (e) => {
      this.addEpochs(reader.result);
    };

    reader.readAsText(file);
  }
}

