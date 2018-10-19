/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenStatePoint, RavenTimeRange } from './index';

export interface RavenStateBand {
  alignLabel: number;
  addTo: boolean;
  baselineLabel: number;
  borderWidth: number;

  // Use in line plot only.
  color: string;
  fill: boolean;
  fillColor: string;

  height: number;
  heightPadding: number;

  // Use in line plot only.
  icon: string;

  id: string;
  isNumeric: boolean;
  label: string;
  labelColor: number[];
  labelFont: string;
  labelPin: string;
  maxTimeRange: RavenTimeRange;
  name: string;
  parentUniqueId: string | null;
  points: RavenStatePoint[];
  possibleStates: string[];

  // Use in line plot only.
  showIcon: boolean;

  showLabelPin: boolean;
  showStateChangeTimes: boolean;
  showTooltip: boolean;
  sourceIds: string[];
  tableColumns: any[]; // TODO: Remove `any`.
  type: string;
}
