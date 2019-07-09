/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { SeqCommandListComponent } from './seq-command-list.component';
import { SeqCommandListModule } from './seq-command-list.module';

describe('SeqCommandListComponent', () => {
  let component: SeqCommandListComponent;
  let fixture: ComponentFixture<SeqCommandListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [SeqCommandListModule],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SeqCommandListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
