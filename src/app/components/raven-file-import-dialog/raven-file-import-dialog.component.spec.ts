import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RavenImportDialogComponent } from './raven-file-import-dialog.component';

describe('RavenImportDialogComponent', () => {
  let component: RavenImportDialogComponent;
  let fixture: ComponentFixture<RavenImportDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RavenImportDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenImportDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
