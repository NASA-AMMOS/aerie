/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { Store, StoreModule } from '@ngrx/store';
import { of } from 'rxjs';

import { RavenActivityDetail } from '../../../shared/models';
import { AdaptationMockService } from '../../../shared/services/adaptation-mock.service';
import { ActivitiesComponent } from './activities.component';
import { ActivitiesModule } from './activities.module';

describe('ActivitiesComponent', () => {
  let component: ActivitiesComponent;
  let fixture: ComponentFixture<ActivitiesComponent>;

  let mockRoute: any = {
    snapshot: {
      data: {},
    },
  };

  const mockRouter = jasmine.createSpyObj('Router', ['navigate']);
  const mockStore = jasmine.createSpyObj('Store', [
    'next',
    'pipe',
    'select',
    'dispatch',
  ]);

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [],
      imports: [
        ActivitiesModule,
        StoreModule.forRoot({}),
        NoopAnimationsModule,
      ],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: Store, useValue: mockStore },
      ],
    });

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ActivitiesComponent);
    component = fixture.componentInstance;
    component.activityTypes = of(
      Object.values(AdaptationMockService.getMockActivityTypes('test')),
    );
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should save a new activity', () => {
    component.activityForm.patchValue({
      activityTypeId: 'test1',
      color: '#7cbfb7',
      constraints: [],
      duration: 10,
      end: 10,
      endTimestamp: '',
      id: '001',
      intent: 'Genenetically modify a donkey to have wings',
      name: 'Hawkey',
      parameters: [],
      sequenceId: 'r578x',
      start: 0,
      startTimestamp: '2017-02-01T15:23:11',
      subActivityIds: [],
      y: null,
    } as RavenActivityDetail);

    spyOn(component, 'onSubmitActivityForm').and.callThrough();

    const form: DebugElement = fixture.debugElement.query(
      By.css('#activity-form'),
    );
    form.triggerEventHandler('submit', null);
    fixture.detectChanges();

    expect(component.onSubmitActivityForm).toHaveBeenCalled();
  });

  xit('should update an existing activity', () => {
    const activityDetail = {
      activityDetail: {
        activityTypeId: 'test1',
        color: '#7cbfb7',
        duration: 10,
        end: 10,
        endTimestamp: '',
        id: 'testy',
        intent: 'Genenetically modify a donkey to have wings',
        name: 'Hawkey',
        sequenceId: 'r578x',
        start: 0,
        startTimestamp: '2017-02-01T15:23:11',
        y: null,
      },
    };

    // TODO: Fix this! It doesn't work update the route
    mockRoute = {
      snapshot: {
        data: {
          activityDetail,
        },
      },
    };

    spyOn(component, 'onSubmitActivityForm').and.callThrough();

    const form: DebugElement = fixture.debugElement.query(
      By.css('#activity-form'),
    );
    form.triggerEventHandler('submit', null);
    fixture.detectChanges();

    expect(component.onSubmitActivityForm).toHaveBeenCalledWith({
      ...activityDetail,
    });
  });
});
