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
import * as monaco from 'monaco-editor';
import { HbCommand } from '../../models/hb-command';
import { HbCommandEditorComponent } from './hb-command-editor.component';
import { HbCommandEditorModule } from './hb-command-editor.module';

describe('HbCommandEditorComponent', () => {
  let component: HbCommandEditorComponent;
  let fixture: ComponentFixture<HbCommandEditorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HbCommandEditorModule],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HbCommandEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should default completionItems to be undefined', () => {
    expect(component.completionItems).toBeUndefined();
  });

  it('should default language to be `command-language`', () => {
    expect(component.language).toBe('command-language');
  });

  it('should default theme to be `command-theme`', () => {
    expect(component.theme).toBe('command-theme');
  });

  it('should default value to be empty string', () => {
    expect(component.value).toBe('');
  });

  it('should convert commands to completion items', () => {
    const commands: HbCommand[] = [
      {
        definitionMaturity: 'UNLOCKED',
        description: 'A test command',
        implementationMaturity: 'FUNCTIONAL',
        name: 'TEST_2_10',
        opcode: '10',
        operationalCategory: 'TEST',
        parameterDefs: [
          {
            default_: 'TRUE_IS_TRUE',
            description: 'True is true',
            mode: null,
            name: 'condition',
            range: "'TEST_IN_USE','TEST_ERROR','TEST_SUCCESS'",
            type: {
              simple: { arraySize: null, baseType: 'STRING' },
              varArray: null,
            },
            units: null,
          },
        ],
        processorString: null,
        restrictedPhases: ['PRE-LAUNCH', 'LAUNCH'],
      },
    ];

    expect(component.completionItems).toBeUndefined();

    component.commands = commands;
    component.ngOnChanges({ commands: new SimpleChange('', commands, false) });
    fixture.detectChanges();

    expect(component.completionItems).toBeDefined();
    expect((component.completionItems[0].insertText as any).value).toBe(
      'TEST_2_10 condition',
    );
    expect(component.completionItems[0].kind).toBe(
      monaco.languages.CompletionItemKind.Text,
    );
    expect(component.completionItems[0].label).toBe('TEST_2_10');
  });
});
