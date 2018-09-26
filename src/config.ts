/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenDefaultBandSettings } from './app/shared/models';
import { environment } from './environments/environment';
import { version } from './environments/version';

export interface ConfigState {
  app: {
    baseUrl: string;
    branch: string;
    commit: string;
    production: boolean;
    version: string;
  };
  hummingbird: {
    // TODO. Add hummingbird specific config here.
  };
  mpsServer: {
    apiUrl: string;
    ravenConfigUrl: string;
    epochsUrl: string;
    ravenUrl: string;
    socketUrl: string;
  };
  raven: {
    defaultBandSettings: RavenDefaultBandSettings;
    itarMessage: string;
    shareableLinkStatesUrl: string;
  };
}

export const config: ConfigState = {
  app: {
    baseUrl: environment.baseUrl,
    branch: version.branch,
    commit: version.commit,
    production: environment.production,
    version: version.version,
  },
  hummingbird: {},
  mpsServer: {
    apiUrl: 'mpsserver/api/v2/fs',
    epochsUrl: 'mpsserver/api/v2/fs-mongodb/leucadia/taifunTest/europaEpoch.csv',
    ravenConfigUrl: 'mpsserver/api/v2/raven_config_file',
    ravenUrl: 'mpsserver/raven',
    socketUrl: 'mpsserver/websocket/v1/topic/main',
  },
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
    itarMessage: '',
    shareableLinkStatesUrl: 'TEST_ATS/STATES',
  },
};
