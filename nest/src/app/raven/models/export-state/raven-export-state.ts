/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenExportBand } from './raven-export-band';
import { RavenExportDefaultBandSettings } from './raven-export-default-band-settings';
import { RavenExportEpoch } from './raven-export-epoch';

export interface RavenExportState {
  bands: RavenExportBand[];
  defaultBandSettings: RavenExportDefaultBandSettings;
  expansionByActivityId: {
    [key: string]: string;
  };
  guides: number[];
  ignoreShareableLinkTimes: boolean;
  inUseEpoch: RavenExportEpoch | null;
  maxTimeRange: {
    end: number;
    start: number;
  };
  name: string;
  pins: {
    name: string;
    sourceId: string;
  }[];
  showDetailsPanel: boolean;
  showLeftPanel: boolean;
  showRightPanel: boolean;
  showSouthBandsPanel: boolean;
  version: string;
  viewTimeRange: {
    end: string;
    start: string;
  };
}
