/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  ConfigState,
  initialState,
  reducer,
} from './config';

import {
  UpdateDefaultBandSettings,
} from './../actions/config';

describe('config reducer', () => {
  let timelineState: ConfigState;

  beforeEach(() => {
    timelineState = initialState;
  });

  it('handle default', () => {
    expect(timelineState).toEqual(initialState);
  });

  it('handle UpdateDefaultBandSettings activityLayout', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ activityLayout: 2 }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        activityLayout: 2,
      },
    });
  });

  it('handle UpdateDefaultBandSettings icon', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ icon: 'triangle' }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        icon: 'triangle',
      },
    });
  });

  it('handle UpdateDefaultBandSettings labelFont', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ labelFont: 'Courier' }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        labelFont: 'Courier',
      },
    });
  });

  it('handle UpdateDefaultBandSettings labelFontSize', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ labelFontSize: 11 }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        labelFontSize: 11,
      },
    });
  });

  it('handle UpdateDefaultBandSettings labelWidth', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ labelWidth: 50 }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        labelWidth: 50,
      },
    });
  });

  it('handle UpdateDefaultBandSettings resourceColor', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ resourceColor: '#00ff00' }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        resourceColor: '#00ff00',
      },
    });
  });

  it('handle UpdateDefaultBandSettings resourceFillColor', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ resourceFillColor: '#ff0000' }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        resourceFillColor: '#ff0000',
      },
    });
  });

  it('handle UpdateDefaultBandSettings showTimeCursor', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ showTimeCursor: true }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        showTimeCursor: true,
      },
    });
  });

  it('handle UpdateDefaultBandSettings showTooltip', () => {
    timelineState = reducer(timelineState, new UpdateDefaultBandSettings({ showTooltip: true }));
    expect(timelineState).toEqual({
      ...initialState,
      defaultBandSettings: {
        ...initialState.defaultBandSettings,
        showTooltip: true,
      },
    });
  });
});
