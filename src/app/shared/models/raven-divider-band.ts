/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenTimeRange } from './index';

/**
 * Note: Divider bands don't use all of these properties.
 * They are just here for easier typing of sub-bands.
 */

export interface RavenDividerBand {
  addTo: boolean;
  color: number[];
  height: number;
  heightPadding: number;
  id: string;
  label: string;
  labelColor: number[];
  labelPin: string;
  maxTimeRange: RavenTimeRange;
  name: string;
  parentUniqueId: string | null;
  points: any[]; // A divider bands should never actually have points.
  showTooltip: boolean;
  sourceIds: string[];
  tableColumns: any[]; // TODO: Remove `any`.
  type: string;
}
