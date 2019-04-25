/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { getMockPlans } from '../../services/plan-mock.service';
import { PlanTableComponent } from './plan-table.component';
import { PlanTableModule } from './plan-table.module';

@Component({
  selector: 'plan-table-test',
  template: `
    <plan-table [plans]="plans"> </plan-table>
  `,
})
class PlanTableTestComponent {
  plans = getMockPlans();

  @ViewChild(PlanTableComponent)
  childComponent: PlanTableComponent;
}

describe('PlanTableComponent', () => {
  let component: PlanTableTestComponent;
  let fixture: ComponentFixture<PlanTableTestComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [PlanTableTestComponent],
      imports: [PlanTableModule, NoopAnimationsModule],
    }).compileComponents();

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PlanTableTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
