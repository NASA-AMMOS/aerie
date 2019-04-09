/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Store, StoreModule } from '@ngrx/store';
import { reducers as rootReducers } from '../../../app-store';
import { ActivityInstance } from '../../../shared/models';
import { doNgOnChanges } from '../../../shared/util';
import { activity } from '../../mocks';
import { reducers } from '../../planning-store';
import { PlanningService } from '../../services/planning.service';
import { ActivityFormFullComponent } from './activity-form-full.component';
import { ActivityFormFullModule } from './activity-form-full.module';

describe('ActivityFormFullComponent', () => {
  let component: ActivityFormFullComponent;
  let fixture: ComponentFixture<ActivityFormFullComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        ActivityFormFullModule,
        NoopAnimationsModule,
        StoreModule.forRoot(rootReducers),
        StoreModule.forFeature('planning', reducers),
      ],
      providers: [PlanningService, Store],
    }).compileComponents();
    fixture = TestBed.createComponent(ActivityFormFullComponent);
    component = fixture.componentInstance;
  }));

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should initialize an invalid form', () => {
    expect(component.form.valid).toBe(false);
  });

  it('form should not have a constraints control if there is no selected activity', () => {
    expect(component.form.controls.constraints).toBeUndefined();
  });

  it('form should not have a parameters control if there is no selected activity', () => {
    expect(component.form.controls.parameters).toBeUndefined();
  });

  describe('setting a selected activity', () => {
    it(' should add a parameters control to the form', () => {
      component.selectedActivity = { ...activity };
      doNgOnChanges(component, ['selectedActivity']);
      expect(component.form.controls.parameters).toBeDefined();
    });

    it('should properly patch the form values with the selected activities properties that exist in the form', () => {
      component.selectedActivity = { ...activity };
      doNgOnChanges(component, ['selectedActivity']);
      expect(component.form.controls.activityType.value).toEqual(
        component.selectedActivity.activityType,
      );
      expect(component.form.controls.duration.value).toEqual(
        component.selectedActivity.duration,
      );
      expect(component.form.controls.intent.value).toEqual(
        component.selectedActivity.intent,
      );
      expect(component.form.controls.name.value).toEqual(
        component.selectedActivity.name,
      );
      expect(component.form.controls.start.value).toEqual(
        component.selectedActivity.start,
      );
    });
  });

  describe('setting a selected activity and isNew to true', () => {
    it('should not add a parameters control to the form', () => {
      component.selectedActivity = { ...activity };
      component.isNew = true;
      doNgOnChanges(component, ['selectedActivity', 'isNew']);
      expect(component.form.controls.parameters).toBeUndefined();
    });

    it('the form values should all be empty', () => {
      component.selectedActivity = { ...activity };
      component.isNew = true;
      doNgOnChanges(component, ['selectedActivity', 'isNew']);
      expect(component.form.controls.activityType.value).toEqual('');
      expect(component.form.controls.duration.value).toEqual(0);
      expect(component.form.controls.intent.value).toEqual('');
      expect(component.form.controls.name.value).toEqual('');
      expect(component.form.controls.start.value).toEqual(0);
    });
  });

  describe('onSubmit is called and the form is valid', () => {
    it('if isNew is false then updateActivity should emit with the form value', () => {
      component.form.clearValidators();
      component.selectedActivity = { ...activity };
      component.isNew = false;
      doNgOnChanges(component, ['selectedActivity', 'isNew']);
      component.updateActivity.subscribe(
        (updatedActivity: ActivityInstance) => {
          expect(updatedActivity).toEqual(activity);
        },
      );
      component.onSubmit(activity);
    });

    it('if isNew is true then createActivity should emit with the form value', () => {
      component.form.clearValidators();
      component.isNew = true;
      doNgOnChanges(component, ['isNew']);
      component.createActivity.subscribe(
        (createdActivity: ActivityInstance) => {
          expect(createdActivity).toEqual(activity);
        },
      );
      component.onSubmit(activity);
    });
  });
});
