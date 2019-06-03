/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  CloseTab,
  CreateTab,
  SwitchTab,
  UpdateTab,
} from '../actions/file.actions';
import { initialState, reducer } from './file.reducer';

const mockFile1 = {
  filename: `New File 11`,
  id: '11',
  text: '',
};

const mockFile2 = {
  filename: `New File 12`,
  id: '12',
  text: '',
};

const mockFile3 = {
  filename: `New File 13`,
  id: '13',
  text: '',
};

const mockStateAfterCreateTab = {
  currentTab: '11',
  openedTabs: { '11': mockFile1 },
};

const emptyState = {
  currentTab: '11',
  openedTabs: {},
};

const mockStateForSwitchTab = {
  currentTab: '12',
  openedTabs: { '12': mockFile2, '13': mockFile3 },
};

describe('File reducer', () => {
  it('handle default', () => {
    expect(initialState).toEqual(initialState);
  });

  it('should handle CreateTab', () => {
    const result = reducer(initialState, new CreateTab());

    expect(result).toEqual(mockStateAfterCreateTab);
  });

  it('should handle CloseTab', () => {
    const result = reducer(mockStateAfterCreateTab, new CloseTab('11'));

    expect(result).toEqual(emptyState);
  });

  it('should handle switchTab', () => {
    let result = reducer(initialState, new CreateTab());
    result = reducer(result, new CreateTab());

    expect(result).toEqual(mockStateForSwitchTab);

    result = reducer(result, new SwitchTab('13'));

    expect(result.currentTab).toBe('13');
  });

  it('should handle updateTab', () => {
    let result = reducer(initialState, new CreateTab());
    result = reducer(result, new UpdateTab('14', 'hello'));

    if (result.openedTabs) expect(result.openedTabs['14'].text).toBe('hello');
  });
});
