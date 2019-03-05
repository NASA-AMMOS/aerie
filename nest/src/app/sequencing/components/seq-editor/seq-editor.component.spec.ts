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
import { commands } from '../../../shared/mocks';
import { keyCommandsByName } from '../../util';
import { SeqEditorComponent } from './seq-editor.component';
import { SeqEditorModule } from './seq-editor.module';

@Component({
  selector: 'seq-editor-test',
  template: `
    <seq-editor [commands]="commands" [commandsByName]="commandsByName">
    </seq-editor>
  `,
})
class SeqEditorTestComponent {
  commands = commands;
  commandsByName = keyCommandsByName(commands);

  @ViewChild(SeqEditorComponent)
  childComponent: SeqEditorComponent;
}

describe('SeqEditorComponent', () => {
  let component: SeqEditorTestComponent;
  let fixture: ComponentFixture<SeqEditorTestComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [SeqEditorTestComponent],
      imports: [SeqEditorModule],
    }).compileComponents();

    await TestBed.compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SeqEditorTestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should properly initialize a Codemirror editor instance', () => {
    expect(component.childComponent.editor).not.toBeNull();
  });

  it(`should properly initialize the custom 'command' Codemirror mode`, () => {
    const editor = component.childComponent.editor as CodeMirror.Editor;
    expect(editor.getOption('mode')).toEqual('command');
  });
});
