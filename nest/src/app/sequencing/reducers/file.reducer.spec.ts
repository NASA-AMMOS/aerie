/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { keyBy } from 'lodash';
import {
  CloseTab,
  CreateTab,
  SwitchTab,
  UpdateChildren,
  UpdateTab,
} from '../actions/file.actions';
import { getChildren } from '../services/file-mock.service';
import { initialState, reducer } from './file.reducer';

const mockFile1 = {
  editors: {
    editor1: {
      currentTab: null,
      id: 'editor1',
      openedTabs: {
        '11': {
          filename: `New File 11`,
          id: '11',
          text: '',
        },
      },
    },
  },
};

const mockFile2 = {
  editors: {
    editor1: {
      currentTab: null,
      id: 'editor1',
      openedTabs: {
        '12': {
          filename: `New File 12`,
          id: '12',
          text: '',
        },
      },
    },
  },
};

const mockFile3 = {
  editors: {
    editor1: {
      currentTab: null,
      id: 'editor1',
      openedTabs: {
        '13': {
          filename: `New File 13`,
          id: '13',
          text: '',
        },
      },
    },
  },
};

const mockStateAfterCreateTab = {
  editors: {
    editor1: {
      currentTab: '11',
      id: 'editor1',
      openedTabs: { '11': mockFile1.editors.editor1.openedTabs[11] },
    },
  },
  files: {
    root: {
      childIds: [],
      content: '',
      id: 'root',
      name: 'root',
      timeCreated: 0,
      timeLastUpdated: 0,
      type: 'directory',
    },
  },
};

const mockStateForSwitchTab = {
  editors: {
    editor1: {
      currentTab: '12',
      id: 'editor1',
      openedTabs: {
        '12': mockFile2.editors.editor1.openedTabs[12],
        '13': mockFile3.editors.editor1.openedTabs[13],
      },
    },
  },
  files: {
    root: {
      childIds: [],
      content: '',
      id: 'root',
      name: 'root',
      timeCreated: 0,
      timeLastUpdated: 0,
      type: 'directory',
    },
  },
};

describe('File reducer', () => {
  it('handle default', () => {
    expect(initialState).toEqual(initialState);
  });

  it('should handle CreateTab', () => {
    const result = reducer(initialState, new CreateTab('editor1'));
    expect(result).toEqual(mockStateAfterCreateTab);
  });

  it('should handle CloseTab', () => {
    const result = reducer(
      mockStateAfterCreateTab,
      new CloseTab('11', 'editor1'),
    );

    expect(result).toEqual(initialState);
  });

  it('should handle switchTab', () => {
    let result = reducer(initialState, new CreateTab('editor1'));
    result = reducer(result, new CreateTab('editor1'));

    expect(result).toEqual(mockStateForSwitchTab);

    result = reducer(result, new SwitchTab('13', 'editor1'));

    expect(result.editors.editor1.currentTab).toBe('13');
  });

  it('should handle updateTab', () => {
    let result = reducer(initialState, new CreateTab('editor1'));
    result = reducer(result, new UpdateTab('14', 'hello', 'editor1'));

    if (result.editors.editor1.openedTabs)
      expect(result.editors.editor1.openedTabs['14'].text).toBe('hello');
  });

  it('should handle UpdateChildren', () => {
    const parentId = 'root';
    const children = getChildren(parentId);
    const childIds = getChildren(parentId).map(child => child.id);
    const result = reducer(
      initialState,
      new UpdateChildren(parentId, children),
    );
    expect(result).toEqual({
      ...initialState,
      files: {
        ...initialState.files,
        [parentId]: {
          ...initialState.files[parentId],
          childIds,
        },
        ...keyBy(children, 'id'),
      },
    });
  });
});
