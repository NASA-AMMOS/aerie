/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivityInstance } from '../../../shared/models';
import { doNgOnChanges } from '../../../shared/util';
import { activity, activityTypes } from '../../mocks';
import { ActivityFormComponent } from './activity-form.component';
import { ActivityFormModule } from './activity-form.module';

describe('ActivityBandComponent', () => {
  let component: ActivityFormComponent;
  let fixture: ComponentFixture<ActivityFormComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ActivityFormModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ActivityFormComponent);
    component = fixture.componentInstance;
    component.activityTypes = activityTypes;
  }));

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('form invalid when empty', () => {
    expect(component.form.valid).toBeFalsy();
  });

  it('form is valid when editing an activity', () => {
    component.activity = activity;
    doNgOnChanges(component, ['activity']);

    expect(component.form.valid).toBeTruthy();
  });

  it('activityType is set to nothing if no selected activity', () => {
    component.activityTypes = activityTypes;

    doNgOnChanges(component, ['selectedActivityType', 'activityType']);

    expect(component.selectedActivityType).toBeNull();
  });

  it('activityType is set to the selected activityType', () => {
    const selectedActivityType = activityTypes[0];
    component.selectedActivityType = selectedActivityType;

    doNgOnChanges(component, ['selectedActivityType']);

    expect(component.form.controls.activityType.value).toBe(
      selectedActivityType.name,
    );
  });

  describe('onSubmit is called and the form is valid', () => {
    it('submitting form when isNew is true emits a createActivity', () => {
      component.form.clearValidators();
      component.activity = activity;
      component.isNew = true;
      doNgOnChanges(component, ['isNew']);

      component.createActivity.subscribe(
        (createdActivity: ActivityInstance) => {
          expect(createdActivity).toEqual(activity);
        },
      );
      component.onSubmit(activity);
    });
    it('submitting form when isNew is false emits an updateActivity', () => {
      component.form.clearValidators();
      component.activity = activity;
      component.isNew = false;
      doNgOnChanges(component, ['activity', 'isNew']);

      component.updateActivity.subscribe(
        (updatedActivity: ActivityInstance) => {
          expect(updatedActivity).toEqual({
            ...activity,
          });
        },
      );
      component.onSubmit({
        ...activity,
        start: (activity.start as number) * 1000,
      });
    });
  });

  it('activityType input is enabled when creating a new activity', () => {
    component.isNew = true;
    doNgOnChanges(component, ['isNew']);
    const result = fixture.debugElement.query(
      By.css('.mat-form-field-disabled'),
    );

    expect(result).toBeDefined();
  });

  it('activityType input is disabled when editing an activity', () => {
    component.form.clearValidators();
    component.isNew = true;
    component.activity = activity;
    doNgOnChanges(component, ['activity', 'isNew']);

    const result = fixture.debugElement.query(
      By.css('.mat-form-field-disabled'),
    );

    expect(result).toBeNull();
  });
});
