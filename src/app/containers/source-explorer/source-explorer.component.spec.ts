/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MatDialogModule, MatMenuModule } from '@angular/material';
import { StoreModule } from '@ngrx/store';

import { SourceExplorerComponent } from './source-explorer.component';

import {
  RavenStateLoadDialogComponent,
  RavenStateSaveDialogComponent,
} from './../../components';

import { metaReducers, reducers } from './../../store';

describe('SourceExplorerComponent', () => {
  let component: SourceExplorerComponent;
  let fixture: ComponentFixture<SourceExplorerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        RavenStateLoadDialogComponent,
        RavenStateSaveDialogComponent,
        SourceExplorerComponent,
      ],
      imports: [
        MatDialogModule,
        MatMenuModule,
        StoreModule.forRoot(reducers, { metaReducers }),
      ],
      schemas: [
        CUSTOM_ELEMENTS_SCHEMA,
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SourceExplorerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
