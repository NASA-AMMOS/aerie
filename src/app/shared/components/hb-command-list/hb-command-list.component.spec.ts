/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { HBCommandListComponent } from './hb-command-list.component';
import { HBCommandListModule } from './hb-command-list.module';

describe('HBCommandListComponent', () => {
  let component: HBCommandListComponent;
  let fixture: ComponentFixture<HBCommandListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HBCommandListModule],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HBCommandListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
