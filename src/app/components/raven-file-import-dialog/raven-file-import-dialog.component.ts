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

  onCancel() {
    this.dialogRef.close({ import: false });
  }

  onChangeFileType(fileType: string) {
    this.fileType = fileType;
  }

  onFileImport() {
    this.dialogRef.close({ name: this.name, fileType: this.fileType, fileData: this.fileData, mappingData: this.mappingData, import: true });
  }

  getFileContent($event: any): void {
    this.readFileData($event.target);
  }

  getMappingFile($event: any): void {
    this.readMappingFile($event.target);
  }

  readFileData(inputValue: any): void {
    const file: File = inputValue.files[0];
    const reader: FileReader = new FileReader();
    reader.onloadend = (e) => {
      this.fileData = reader.result;
    };

    reader.readAsText(file);
  }

  readMappingFile(inputValue: any): void {
    const file: File = inputValue.files[0];
    const reader: FileReader = new FileReader();
    reader.onloadend = (e) => {
      this.mappingData = reader.result;
    };

    reader.readAsText(file);
  }
}
