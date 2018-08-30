/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, ViewChild } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { HBCommandLoaderComponent } from './hb-command-loader.component';
import { HBCommandLoaderModule } from './hb-command-loader.module';

import * as mpsServerMocks from '../../mocks/mps-server';

describe('HBCommandLoaderComponent', () => {
  let component: HBCommandLoaderTestComponent;
  let fixture: ComponentFixture<HBCommandLoaderTestComponent>;
  let element: HTMLElement;

  /**
   * Update an input on a component.
   * @param input The @Input property to update on the component
   * @param value The value to set on the input
   */
  function updateInput(input: string, value: any) {
    component[input] = value;
    fixture.detectChanges();
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [HBCommandLoaderTestComponent],
      imports: [HBCommandLoaderModule, NoopAnimationsModule],
    }).compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(HBCommandLoaderTestComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should select the option with a value of `selectedId`', fakeAsync(() => {
    const id = 'TEST_2';
    updateInput('selectedId', id);
    const select: HTMLSelectElement | null = element.querySelector('mat-select');
    const value: string | null = select ? select.getAttribute('ng-reflect-value') : null;
    expect(value).toBe(id);
  }));

});

@Component({
  selector: 'command-loader-test',
  template: `
    <hb-command-loader
      [dictionaries]="dictionaries"
      [selectedId]="selectedId"
      (onSelected)="selectedDictionaryChanged">
    </hb-command-loader>
  `,
})
class HBCommandLoaderTestComponent {
  dictionaries: any[] = mpsServerMocks.commandDictionaryList;
  selectedId: string | null = null;

  @ViewChild(HBCommandLoaderComponent)
  component: HBCommandLoaderComponent;

  selectedDictionaryChanged = jasmine.createSpy(
    'HBCommandLoaderTestComponent::selectedDictionaryChanged',
  );
}
