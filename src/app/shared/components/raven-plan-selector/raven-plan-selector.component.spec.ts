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

import { RavenPlan } from '../../models/raven-plan';
import { PlanMockService } from '../../services/plan-mock.service';
import { RavenPlanSelectorComponent } from './raven-plan-selector.component';
import { RavenPlanSelectorModule } from './raven-plan-selector.module';

@Component({
  selector: 'raven-plan-selector-test',
  template: `
    <raven-plan-selector
      [plans]="plans"
      (selectPlanClicked)="select($event)">
    </raven-plan-selector>
  `,
})
class RavenPlanSelectorTestComponent {
  plans = PlanMockService.getMockData();

  @ViewChild(RavenPlanSelectorComponent)
  childComponent: RavenPlanSelectorComponent;

  select(id: string) {}
}

describe('RavenPlanSelectorComponent', () => {
  let component: RavenPlanSelectorTestComponent;
  let fixture: ComponentFixture<RavenPlanSelectorTestComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [RavenPlanSelectorTestComponent],
      imports: [RavenPlanSelectorModule, NoopAnimationsModule],
    }).compileComponents();

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenPlanSelectorTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit the selectPlanClicked event', () => {
    spyOn(component, 'select');
    component.childComponent.onSelectionChanged(component.plans[0].id);
    expect(component.select).toHaveBeenCalled();
  });

  describe('no selectedPlan', () => {
    it('should return a properly formatted startDate when there is no selectedPlan', () => {
      expect(component.childComponent.startDate).toBe('from');
    });

    it('should return a properly formatted endDate when there is no selectedPlan', () => {
      expect(component.childComponent.endDate).toBe('to');
    });

    it('should return an empty selectedId if there is no selectedPlan', () => {
      expect(component.childComponent.selectedId).toBe('');
    });
  });

  describe('selectedPlan', () => {
    let selectedPlan: RavenPlan;

    beforeEach(() => {
      selectedPlan = { ...component.plans[0] };
      component.childComponent.selectedPlan = selectedPlan;
      fixture.detectChanges();
    });

    it('should return a properly formatted startDate', () => {
      expect(component.childComponent.startDate).toBe('12.17.1995');
    });

    it('should return a properly formatted endDate', () => {
      expect(component.childComponent.endDate).toBe('12.18.1995');
    });

    it('should return the selectedId if there is a selectedPlan', () => {
      expect(component.childComponent.selectedId).toBe(selectedPlan.id);
    });
  });
});
