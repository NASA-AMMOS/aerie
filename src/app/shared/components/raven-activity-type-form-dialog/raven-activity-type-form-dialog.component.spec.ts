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

import { RavenActivityType } from '../../models/raven-activity-type';
import { RavenActivityTypeFormDialogComponent } from './raven-activity-type-form-dialog.component';
import { RavenActivityTypeFormDialogModule } from './raven-activity-type-form-dialog.module';

@Component({
  selector: 'raven-activity-type-form-dialog-test',
  template: `
    <raven-activity-type-form-dialog>
    </raven-activity-type-form-dialog>
  `,
})
class RavenActivityTypeFormDialogTestComponent {
  data: RavenActivityType | null;

  @ViewChild(RavenActivityTypeFormDialogComponent)
  component: RavenActivityTypeFormDialogComponent;
}

describe('RavenActivityTypeFormDialogComponent', () => {
  let component: RavenActivityTypeFormDialogTestComponent;
  let fixture: ComponentFixture<RavenActivityTypeFormDialogTestComponent>;
  let element: HTMLElement;
  let data: any;

  const mockDialogRef = {
    close: jasmine.createSpy('close'),
  };

  beforeEach(async () => {
    data = {
      id: 'test0',
      name: 'Test 0',
      start: '16:53',
    };

    TestBed.configureTestingModule({
      declarations: [RavenActivityTypeFormDialogTestComponent],
      imports: [RavenActivityTypeFormDialogModule, NoopAnimationsModule],
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
    fixture = TestBed.createComponent(RavenActivityTypeFormDialogTestComponent);
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

    expect(component.component.onSubmit).toHaveBeenCalledWith({
      name: data.name,
      start: data.start,
    });

    expect(mockDialogRef.close).toHaveBeenCalledWith(data);
  });

  it('should fail validation', () => {
    const name: AbstractControl = component.component.form.controls['name'];

    name.setValue('');
    name.updateValueAndValidity();

    expect(name.valid).toBe(false);
  });
});
