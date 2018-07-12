/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  RavenDefaultBandSettings,
} from './app/shared/models';

/**
 * Type interface schema for Raven's configuration.
 * This object is loaded into the store (in various reducers) upon application load.
 */

export interface RavenConfig {
  baseSocketUrl: string;
  baseSourcesUrl: string;
  defaultBandSettings: RavenDefaultBandSettings;
  epochsUrl: string;
  itarMessage: string;
  shareableLinkStatesUrl: string;
  showTimeCursor: boolean;
}

const ravenConfig: RavenConfig = {
  baseSocketUrl: 'mpsserver/websocket/v1/topic/main',
  baseSourcesUrl: 'mpsserver/api/v2/fs',
  defaultBandSettings: {
    activityLayout: 0,
    icon: 'circle',
    iconEnabled: false,
    labelFont: 'Georgia',
    labelFontSize: 9,
    labelWidth: 100,
    resourceColor: '#000000',
    resourceFillColor: '#000000',
    showTimeCursor: false,
    showTooltip: true,
  },
  epochsUrl: 'mpsserver/api/v2/fs-mongodb/leucadia/taifunTest/europaEpoch.csv',
  itarMessage: '',
  shareableLinkStatesUrl: 'TEST_ATS/STATES',
  showTimeCursor: false,
};

export default ravenConfig;
