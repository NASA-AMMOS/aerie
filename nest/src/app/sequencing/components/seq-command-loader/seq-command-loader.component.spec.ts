/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Component, ViewChild } from '@angular/core';
import {
  async,
  ComponentFixture,
  fakeAsync,
  TestBed,
} from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { mockCommandDictionaryList } from '../../../shared/services/command-dictionary-mock.service';
import { SeqCommandLoaderComponent } from './seq-command-loader.component';
import { SeqCommandLoaderModule } from './seq-command-loader.module';

@Component({
  selector: 'command-loader-test',
  template: `
    <seq-command-loader
      [dictionaries]="dictionaries"
      [selectedId]="selectedId"
      (onSelected)="(selectedDictionaryChanged)"
    >
    </seq-command-loader>
  `,
})
class SeqCommandLoaderTestComponent {
  dictionaries: any[] = mockCommandDictionaryList;
  selectedId: string | null = null;

  @ViewChild(SeqCommandLoaderComponent)
  component: SeqCommandLoaderComponent;

  selectedDictionaryChanged = jasmine.createSpy(
    'SeqCommandLoaderTestComponent::selectedDictionaryChanged',
  );
}

describe('SeqCommandLoaderComponent', () => {
  let component: SeqCommandLoaderTestComponent;
  let fixture: ComponentFixture<SeqCommandLoaderTestComponent>;
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
      declarations: [SeqCommandLoaderTestComponent],
      imports: [SeqCommandLoaderModule, NoopAnimationsModule],
    }).compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(SeqCommandLoaderTestComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should select the option with a value of `selectedId`', fakeAsync(() => {
    const id = 'TEST_2';
    updateInput('selectedId', id);
    const select: HTMLSelectElement | null = element.querySelector(
      'mat-select',
    );
    const value: string | null = select
      ? select.getAttribute('ng-reflect-value')
      : null;
    expect(value).toBe(id);
  }));
});
