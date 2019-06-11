/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import keyBy from 'lodash-es/keyBy';
import { mpsCommands } from '../../mocks';
import { SeqEditorComponent } from './seq-editor.component';
import { SeqEditorModule } from './seq-editor.module';

describe('SeqEditorComponent', () => {
  let component: SeqEditorComponent;
  let fixture: ComponentFixture<SeqEditorComponent>;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [SeqEditorModule],
    }).compileComponents();

    fixture = TestBed.createComponent(SeqEditorComponent);
    component = fixture.componentInstance;

    const commands = mpsCommands;
    const commandsByName = keyBy(mpsCommands, 'name');

    component.commands = commands;
    component.commandsByName = commandsByName;
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SeqEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
