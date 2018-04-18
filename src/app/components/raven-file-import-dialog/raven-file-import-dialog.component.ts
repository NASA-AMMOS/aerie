import {
  Component,
  Inject,
} from '@angular/core';

import {
  MAT_DIALOG_DATA,
  MatDialogRef,
} from '@angular/material';

@Component({
  selector: 'raven-file-import-dialog',
  styleUrls: ['./raven-file-import-dialog.component.css'],
  templateUrl: './raven-file-import-dialog.component.html',
})
export class RavenFileImportDialogComponent {

  name: string;
  fileType: string;
  fileData: string;
  mappingData = '';

  constructor(
    public dialogRef: MatDialogRef<RavenFileImportDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) { }

  getFile($event: any): void {
    this.readFile($event.target, false);
  }

  getMapping($event: any): void {
    this.readFile($event.target, true);
  }
  onCancel() {
    this.dialogRef.close({ import: false });
  }

  onChangeFileType(fileType: string) {
    this.fileType = fileType;
  }

  onFileImport() {
    this.dialogRef.close({ name: this.name, fileType: this.fileType, fileData: this.fileData, mappingData: this.mappingData, import: true });
  }

  readFile(inputValue: any, mapping: boolean): void {
    const reader: FileReader = new FileReader();
    reader.onloadend = (e) => {
      if (mapping) {
        this.mappingData = reader.result;
      } else {
        this.fileData = reader.result;
      }
    };

    reader.readAsText(inputValue.files[0]);
  }

}
