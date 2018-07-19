/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  RavenActivityPoint,
  RavenTimeRange,
} from './index';

export interface RavenActivityBand {
  activityHeight: number;
  activityStyle: number;
  addTo: boolean;
  alignLabel: number;
  baselineLabel: number;
  borderWidth: number;
  filterTarget: string | null;
  height: number;
  heightPadding: number;
  icon: string | null;
  id: string;
  label: string;
  labelColor: number[];
  labelFont: string;
  labelPin: string;
  layout: number;
  legend: string;
  maxTimeRange: RavenTimeRange;
  minorLabels: string[];
  name: string;
  parentUniqueId: string | null;
  points: RavenActivityPoint[];
  showLabel: boolean;
  showLabelPin: boolean;
  showTooltip: boolean;
  sourceIds: string[];
  tableColumns: any[]; // TODO: Remove `any`.
  trimLabel: boolean;
  type: string;
}
