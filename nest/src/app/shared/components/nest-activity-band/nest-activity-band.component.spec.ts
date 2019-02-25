/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NestActivityBandComponent } from './nest-activity-band.component';
import { NestActivityBandModule } from './nest-activity-band.module';

describe('NestActivityBandComponent', () => {
  let component: NestActivityBandComponent;
  let fixture: ComponentFixture<NestActivityBandComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [NestActivityBandModule],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NestActivityBandComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });
});
