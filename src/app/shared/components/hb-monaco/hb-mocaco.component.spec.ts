/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { SimpleChange } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { HbMonacoComponent } from './hb-monaco.component';
import { HbMonacoModule } from './hb-monaco.module';

describe('HbMonacoComponent', () => {
  let component: HbMonacoComponent;
  let fixture: ComponentFixture<HbMonacoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HbMonacoModule],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HbMonacoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default completionItems to be undefined', () => {
    expect(component.completionItems).toBeUndefined();
  });

  it('should default language to be undefined', () => {
    expect(component.language).toBeUndefined();
  });

  it('should default options to be vs', () => {
    expect(component.options).toBeUndefined();
  });

  it('should default theme to be vs', () => {
    expect(component.theme).toBe('vs');
  });

  it('should default themeData to be undefined', () => {
    expect(component.themeData).toBeUndefined();
  });

  it('should default tokens to be undefined', () => {
    expect(component.tokens).toBeUndefined();
  });

  it('should default value to be empty string', () => {
    expect(component.value).toBe('');
  });

  it('should update the value of in the editor', () => {
    const value = 'this is a test';
    component.value = value;
    component.ngOnChanges({ value: new SimpleChange('', value, false) });
    fixture.detectChanges();
    expect(component.MonacoInstance.getValue()).toBe(value);
  });

  it('should emit when changes are done', () => {
    let called = false;
    component.changes.subscribe(() => (called = true));
    component.MonacoInstance.setValue('change value');
    expect(called).toBe(true);
  });
});
