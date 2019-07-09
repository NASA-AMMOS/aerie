/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenDefaultBandSettings } from './app/raven/models';
import { NavigationDrawerStates } from './app/shared/actions/config.actions';
import { NestModule } from './app/shared/models';
import { environment } from './environments/environment';
import { version } from './environments/version';

export interface ConfigState {
  app: {
    adaptationServiceBaseUrl: string;
    baseUrl: string;
    packageJsonVersion: string;
    planServiceBaseUrl: string;
    production: boolean;
    sequencingServiceBaseUrl: string;
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
    adaptationServiceBaseUrl: environment.adaptationServiceBaseUrl,
    baseUrl: environment.baseUrl,
    packageJsonVersion: version.packageJsonVersion,
    planServiceBaseUrl: environment.planServiceBaseUrl,
    production: environment.production,
    sequencingServiceBaseUrl: environment.sequencingServiceBaseUrl,
    version: version.version,
  },
  appModules: [
    {
      icon: 'event',
      path: 'plans',
      title: 'Merlin',
      version: '0.0.1',
    },
    {
      icon: 'dns',
      path: 'falcon',
      title: 'Falcon',
      version: '0.0.1',
    },
    {
      icon: 'poll',
      path: 'raven',
      title: 'RAVEN',
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
      activityInitiallyHidden: false,
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
