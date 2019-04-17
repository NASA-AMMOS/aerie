/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivityInstance } from '../../../shared/models';
import { doNgOnChanges } from '../../../shared/util';
import { activity, activityTypes } from '../../mocks';
import { ActivityFormComponent } from './activity-form.component';
import { ActivityFormModule } from './activity-form.module';

fdescribe('ActivityBandComponent', () => {
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

  it('activityType is set to the first of activityTypes if not selected', () => {
    const expectedActivityType = activityTypes[0];
    component.ngOnInit();

    expect(component.selectedActivityType).toBe(expectedActivityType);
    expect(component.form.controls.activityType.value).toBe(
      expectedActivityType.activityClass,
    );
  });

  it('activityType is set to the selected activityType', () => {
    const selectedActivityType = activityTypes[0];
    component.selectedActivityType = selectedActivityType;
    component.ngOnInit();

    expect(component.form.controls.activityType.value).toBe(
      selectedActivityType.activityClass,
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
        start: activity.start * 1000,
      });
    });
  });
});
