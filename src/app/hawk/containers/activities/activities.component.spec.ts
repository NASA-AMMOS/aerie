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

import { ActivitiesComponent } from './activities.component';
import { ActivitiesModule } from './activities.module';

import { AdaptationMockService } from '../../../shared/services/adaptation-mock.service';

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
    component.form.patchValue({
      activityTypeId: 'test1',
      duration: '11:11',
      intent: 'Genenetically modify a donkey to have wings',
      name: 'Hawkey',
      sequenceId: 'r578x',
      start: '2017-02-01T15:23:11',
    });

    spyOn(component, 'onSubmit').and.callThrough();

    const form: DebugElement = fixture.debugElement.query(
      By.css('#activity-form'),
    );
    form.triggerEventHandler('submit', null);
    fixture.detectChanges();

    expect(component.onSubmit).toHaveBeenCalled();
  });

  xit('should update an existing activity', () => {
    const activityDetail = {
      activityDetail: {
        activityTypeId: 'test1',
        duration: '11:11',
        id: 'testy',
        intent: 'Genenetically modify a donkey to have wings',
        name: 'Hawkey',
        sequenceId: 'r578x',
        start: '2017-02-01T15:23:11',
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

    spyOn(component, 'onSubmit').and.callThrough();

    const form: DebugElement = fixture.debugElement.query(
      By.css('#activity-form'),
    );
    form.triggerEventHandler('submit', null);
    fixture.detectChanges();

    expect(component.onSubmit).toHaveBeenCalledWith({
      ...activityDetail,
    });
  });
});
