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
import { ActivityType, StringTMap } from '../../../shared/models';
import { getMockActivityTypes } from '../../services/adaptation-mock.service';
import { ActivityTypeListComponent } from './activity-type-list.component';
import { ActivityTypeListModule } from './activity-type-list.module';

@Component({
  selector: 'activity-type-list-test',
  template: `
    <activity-type-list
      [activityTypes]="activityTypesList"
    >
    </activity-type-list>
  `,
})
class ActivityTypeListTestComponent {
  activityTypes: StringTMap<ActivityType> = getMockActivityTypes();
  activityTypesList: ActivityType[] = Object.values(this.activityTypes);

  @ViewChild(ActivityTypeListComponent)
  component: ActivityTypeListComponent;
}

describe('ActivityTypeListComponent', () => {
  let component: ActivityTypeListTestComponent;
  let fixture: ComponentFixture<ActivityTypeListTestComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [ActivityTypeListTestComponent],
      imports: [ActivityTypeListModule, NoopAnimationsModule],
    });

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ActivityTypeListTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
