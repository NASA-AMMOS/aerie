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
  HBCommandLoaderComponent,
} from './hb-command-loader.component';

import {
  HBCommandLoaderModule,
} from './hb-command-loader.module';

import {
  HBCommandDictionary,
} from '../../models/hb-command-dictionary';

import {
  mpsServerCommandDictionaryList,
} from '../../../shared/mocks/mps-server-command-dictionary-list';

describe('HBCommandLoaderComponent', () => {
  let component: HBCommandLoaderComponent;
  let fixture: ComponentFixture<HBCommandLoaderComponent>;
  let element: HTMLElement;

  /**
   * Update an input on a component.
   * @param input The @Input property to update on the component
   * @param value The value to set on the input
   */
  function updateInput(input: string, value: any) {
    component[input] = value;
    component.ngOnChanges({
      [input]: new SimpleChange(null, value, true),
    });
    fixture.detectChanges();
  }

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [HBCommandLoaderModule],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HBCommandLoaderComponent);
    element = fixture.nativeElement;
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });

  it('should default dictionaries to []', () => {
    expect(component.dictionaries).toEqual([]);
  });

  it('should default selectedId to null', () => {
    expect(component.selectedId).toBeNull();
  });

  describe('Inputs and Outputs', () => {
    beforeEach(() => {
      updateInput('dictionaries', mpsServerCommandDictionaryList);
      updateInput('selectedId', mpsServerCommandDictionaryList[1].id);
    });

    it('should update the list of dictionaries in the grid when they are changed', () => {
      const rows: NodeListOf<Element> = element.querySelectorAll('.ag-body-container > .ag-row');
      expect(rows.length).toBe(2);
    });

    it('should output selectedDictionaryChanged when a command is selected', (done: Function) => {
      updateInput('selectedId', mpsServerCommandDictionaryList[0].id);

      component.selectedDictionaryChanged.subscribe((selected: HBCommandDictionary) => {
        expect(selected).toEqual({
          ...mpsServerCommandDictionaryList[0],
          selected: true,
        });
        done();
      });

      const firstRow = component.agGrid.api.getFirstRenderedRow();
      component.agGrid.api.selectIndex(firstRow, false, false);
    });

    it('should select a row in the grid when selectedId is changed', (done: Function) => {
      const id = mpsServerCommandDictionaryList[0].id;

      // onSelectionChanged gets called by ag-grid when a new selection is made,
      // thus we can be sure that the UI has been updated when it is called.
      // This makes it a great place to assert that our new selection has been
      // applied. Overriding the method is OK because all it does is emit.
      component.onSelectionChanged = function() {
        const rows = component.agGrid.api.getSelectedRows();
        expect(rows.length).toBe(1);
        expect(rows[0].id).toBe(id);
        done();
      };

      updateInput('selectedId', id);
    });
  });
});
