/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { ConfigState } from '../../../config';
import {
  NavigationDrawerStates,
  ToggleNestNavigationDrawer,
  UpdateDefaultBandSettings,
  UpdateMpsServerSettings,
  UpdateRavenSettings,
} from '../actions/config.actions';
import { initialState, reducer, toggleDrawer } from './config.reducer';

describe('config reducer', () => {
  let configState: ConfigState;

  beforeEach(() => {
    configState = initialState;
  });

  it('handle default', () => {
    expect(configState).toEqual(initialState);
  });

  it('handle UpdateDefaultBandSettings activityLayout', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ activityLayout: 2 }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          activityLayout: 2,
        },
      },
    });
  });

  it('handle UpdateDefaultBandSettings icon', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ icon: 'triangle' }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          icon: 'triangle',
        },
      },
    });
  });

  it('handle UpdateDefaultBandSettings labelFont', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ labelFont: 'Courier' }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          labelFont: 'Courier',
        },
      },
    });
  });

  it('handle UpdateDefaultBandSettings labelFontSize', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ labelFontSize: 11 }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          labelFontSize: 11,
        },
      },
    });
  });

  it('handle UpdateDefaultBandSettings labelWidth', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ labelWidth: 50 }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          labelWidth: 50,
        },
      },
    });
  });

  it('handle UpdateDefaultBandSettings resourceColor', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ resourceColor: '#00ff00' }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          resourceColor: '#00ff00',
        },
      },
    });
  });

  it('handle UpdateDefaultBandSettings resourceFillColor', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ resourceFillColor: '#ff0000' }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          resourceFillColor: '#ff0000',
        },
      },
    });
  });

  it('handle UpdateDefaultBandSettings showTooltip', () => {
    const result = reducer(
      configState,
      new UpdateDefaultBandSettings({ showTooltip: true }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        defaultBandSettings: {
          ...initialState.raven.defaultBandSettings,
          showTooltip: true,
        },
      },
    });
  });

  it('handle UpdateMpsServerSettings epochsUrl', () => {
    const result = reducer(
      configState,
      new UpdateMpsServerSettings({
        epochsUrl: 'mpsserver/api/v2/fs-mongodb/leucadia/someEpoch.csv',
      }),
    );
    expect(result).toEqual({
      ...initialState,
      mpsServer: {
        ...initialState.mpsServer,
        epochsUrl: 'mpsserver/api/v2/fs-mongodb/leucadia/someEpoch.csv',
      },
    });
  });

  it('handle UpdateRavenSettings itarMessage', () => {
    const result = reducer(
      configState,
      new UpdateRavenSettings({ itarMessage: 'test itar message' }),
    );
    expect(result).toEqual({
      ...initialState,
      raven: {
        ...initialState.raven,
        itarMessage: 'test itar message',
      },
    });
  });

  it('should toggle the state of the navigation drawer', () => {
    expect(configState.navigationDrawerState).toBe(
      NavigationDrawerStates.Collapsed,
    );

    let result = reducer(configState, new ToggleNestNavigationDrawer());
    expect(result.navigationDrawerState).toBe(NavigationDrawerStates.Closed);

    result = reducer(result, new ToggleNestNavigationDrawer());
    expect(result.navigationDrawerState).toBe(NavigationDrawerStates.Collapsed);
  });

  it('should collapse a closed drawer', () => {
    const newConfigState = {
      ...configState,
      navigationDrawerState: NavigationDrawerStates.Closed,
    };
    const result = toggleDrawer(newConfigState);
    expect(result).toBe(NavigationDrawerStates.Collapsed);
  });

  it('should close a collapsed drawer', () => {
    const newConfigState = {
      ...configState,
      navigationDrawerState: NavigationDrawerStates.Collapsed,
    };
    const result = toggleDrawer(newConfigState);
    expect(result).toBe(NavigationDrawerStates.Closed);
  });
});
