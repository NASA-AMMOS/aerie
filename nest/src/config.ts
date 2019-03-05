/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { NavigationDrawerStates } from './app/shared/actions/config.actions';
import { NestModule, RavenDefaultBandSettings } from './app/shared/models';
import { environment } from './environments/environment';
import { version } from './environments/version';

export interface ConfigState {
  app: {
    apiBaseUrl: string;
    baseUrl: string;
    packageJsonVersion: string;
    production: boolean;
    version: string;
  };
  appModules: NestModule[];
  mpsServer: {
    apiUrl: string;
    ravenConfigUrl: string;
    epochsUrl: string;
    ravenUrl: string;
    socketUrl: string;
  };
  navigationDrawerState: NavigationDrawerStates;
  raven: {
    defaultBandSettings: RavenDefaultBandSettings;
    excludeActivityTypes: string[];
    ignoreShareableLinkTimes: boolean;
    itarMessage: string;
    shareableLinkStatesUrl: string;
  };
  sequencing: {
    // TODO. Add sequencing specific config here.
  };
}

export const config: ConfigState = {
  app: {
    apiBaseUrl: environment.apiBaseUrl,
    baseUrl: environment.baseUrl,
    packageJsonVersion: version.packageJsonVersion,
    production: environment.production,
    version: version.version,
  },
  appModules: [
    {
      icon: 'event',
      path: 'plans',
      title: 'Planning',
      version: '0.0.1',
    },
    {
      icon: 'dns',
      path: 'sequencing',
      title: 'Sequencing',
      version: '0.0.1',
    },
    {
      icon: 'poll',
      path: 'raven',
      title: 'Visualization',
      version: version.packageJsonVersion,
    },
  ],
  mpsServer: {
    apiUrl: 'mpsserver/api/v2/fs',
    epochsUrl: '',
    ravenConfigUrl: 'mpsserver/api/v2/raven_config_file',
    ravenUrl: 'mpsserver/raven',
    socketUrl: 'mpsserver/websocket/v1/topic/main',
  },
  navigationDrawerState: NavigationDrawerStates.Collapsed,
  raven: {
    defaultBandSettings: {
      activityLayout: 0,
      icon: 'circle',
      iconEnabled: false,
      labelFont: 'Georgia',
      labelFontSize: 9,
      labelWidth: 150,
      resourceColor: '#000000',
      resourceFillColor: '#000000',
      showLastClick: true,
      showTooltip: true,
    },
    excludeActivityTypes: [],
    ignoreShareableLinkTimes: false,
    itarMessage: '',
    shareableLinkStatesUrl: 'TEST_ATS/STATES',
  },
  sequencing: {},
};
