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
import { AbstractControl } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Plan } from '../../../../../../schemas/types/ts';
import { RavenPlanFormDialogData } from '../../models/raven-plan-form-dialog-data';
import { RavenPlanFormDialogComponent } from './raven-plan-form-dialog.component';
import { RavenPlanFormDialogModule } from './raven-plan-form-dialog.module';

@Component({
  selector: 'raven-plan-form-dialog-test',
  template: `
    <raven-plan-form-dialog> </raven-plan-form-dialog>
  `,
})
class RavenPlanFormDialogTestComponent {
  data: Plan | null;

  @ViewChild(RavenPlanFormDialogComponent)
  component: RavenPlanFormDialogComponent;
}

describe('RavenPlanFormDialogComponent', () => {
  let component: RavenPlanFormDialogTestComponent;
  let fixture: ComponentFixture<RavenPlanFormDialogTestComponent>;
  let element: HTMLElement;
  let data: RavenPlanFormDialogData;
  let selectedPlan: Plan;

  const mockDialogRef = {
    close: jasmine.createSpy('close'),
  };

  beforeEach(async () => {
    selectedPlan = {
      adaptationId: '',
      endTimestamp: '',
      id: '',
      name: '',
      startTimestamp: '',
    };

    data = {
      adaptations: [],
      selectedPlan,
    };

    TestBed.configureTestingModule({
      declarations: [RavenPlanFormDialogTestComponent],
      imports: [RavenPlanFormDialogModule, NoopAnimationsModule],
      providers: [
        {
          provide: MatDialogRef,
          useValue: mockDialogRef,
        },
        {
          provide: MAT_DIALOG_DATA,
          useValue: data,
        },
      ],
    });

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RavenPlanFormDialogTestComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should pass data through', () => {
    spyOn(component.component, 'onSubmit').and.callThrough();

    const buttons: any = element.querySelectorAll('button');
    if (buttons.length) {
      buttons[1].click();
    }

    const expected = { ...selectedPlan };
    delete expected['id'];

    expect(component.component.onSubmit).toHaveBeenCalledWith(expected);
  });

  it('should fail validation', () => {
    const name: AbstractControl = component.component.form.controls['name'];

    name.setValue('');
    name.updateValueAndValidity();

    expect(name.valid).toBe(false);
  });
});
