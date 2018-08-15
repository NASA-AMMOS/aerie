/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  SimpleChange,
} from '@angular/core';
import {
  async,
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';

import {
  HBCodeMirrorComponent,
} from './hb-code-mirror.component';

describe('HBCodeMirrorComponent', () => {
  let component: HBCodeMirrorComponent;
  let fixture: ComponentFixture<HBCodeMirrorComponent>;

  beforeEach(async(() => {
    TestBed
      .configureTestingModule({
        declarations: [ HBCodeMirrorComponent ],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HBCodeMirrorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should default lineNumbers to be true', () => {
    expect(component.lineNumbers).toBe(true);
  });

  it('should default mode to be empty string', () => {
    expect(component.mode).toBe('');
  });

  it('should default value to be empty string', () => {
    expect(component.value).toBe('');
  });

  it('should pass the default lineNumbers to the codeMirror instance', () => {
    expect(component.codeMirrorInstance.getOption('lineNumbers')).toBe(true);
  });

  it('should pass the default mode to the codeMirror instance', () => {
    expect(component.codeMirrorInstance.getOption('mode')).toBe('');
  });

  it('should pass the default value to the codeMirror instance', () => {
    expect(component.codeMirrorInstance.getOption('value')).toBe('');
  });

  it('should pass value changes to the codeMirror instance', () => {
    const value = 'some code in codemirror!';

    component.value = value;
    component.ngOnChanges({
      value: new SimpleChange('', value, false),
    });

    fixture.detectChanges();
    expect(component.codeMirrorInstance.getValue()).toBe(value);
  });

  it('should pass mode changes to the codeMirror instance', () => {
    const mode = 'javascript';

    component.mode = mode;
    component.ngOnChanges({
      mode: new SimpleChange('', mode, false),
    });

    fixture.detectChanges();
    expect(component.codeMirrorInstance.getOption('mode')).toBe(mode);
  });

  it('should pass lineNumbers changes to the codeMirror instance', () => {
    const lineNumbers = false;

    component.lineNumbers = lineNumbers;
    component.ngOnChanges({
      lineNumbers: new SimpleChange('', lineNumbers, false),
    });

    fixture.detectChanges();
    expect(component.codeMirrorInstance.getOption('lineNumbers')).toBe(lineNumbers);
  });

  it('should emit when beforeChange event', () => {
    let called = false;

    component.beforeChange.subscribe(() => called = true);
    component.codeMirrorInstance.setValue('change value');

    expect(called).toBe(true);
  });

  it('should emit when change event', () => {
    let called = false;

    component.change.subscribe(() => called = true);
    component.codeMirrorInstance.setValue('change value');

    expect(called).toBe(true);
  });

  it('should emit when changes event', () => {
    let called = false;

    component.changes.subscribe(() => called = true);
    component.codeMirrorInstance.setValue('change value');

    expect(called).toBe(true);
  });

});
