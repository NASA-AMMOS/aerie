/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

export interface RavenExportActivitySubBand {
  activityFilter?: string;
  activityHeight: number;
  activityLabelFontSize: number;
  activityStyle: number;
  addTo: boolean;
  alignLabel: number;
  baselineLabel: number;
  borderWidth: number;
  filterTarget: string | null;
  height: number;
  heightPadding: number;
  icon: string | null;
  label: string;
  labelColor: string | number[];
  labelFont: string;
  labelPin: string;
  layout: number;
  legend: string;
  maxTimeRange: {
    end: number;
    start: number;
  };
  minorLabels: string[];
  name: string;
  points: never[]; // empty array
  showActivityTimes: boolean;
  showLabel: boolean;
  showLabelPin: boolean;
  showTooltip: boolean;
  sourceIds: string[];
  tableColumns: any[];
  timeDelta?: number;
  trimLabel: boolean;
  type: string;
}
