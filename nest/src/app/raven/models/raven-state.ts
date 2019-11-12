/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { StringTMap, TimeRange } from '../models';
import {
  RavenCompositeBand,
  RavenDefaultBandSettings,
  RavenPin,
  RavenTimeRangeDoy,
} from './index';
import { RavenEpoch } from './raven-epoch';

export interface RavenState {
  bands: RavenCompositeBand[];
  expansionByActivityId: StringTMap<string>;
  defaultBandSettings: RavenDefaultBandSettings;
  ignoreShareableLinkTimes: boolean;
  inUseEpoch: RavenEpoch | null;
  guides: number[];
  maxTimeRange: TimeRange;
  name: string;
  pins: RavenPin[];
  showDetailsPanel: boolean;
  showLeftPanel: boolean;
  showRightPanel: boolean;
  showSouthBandsPanel: boolean;
  version: string;
  viewTimeRange: RavenTimeRangeDoy;
}
