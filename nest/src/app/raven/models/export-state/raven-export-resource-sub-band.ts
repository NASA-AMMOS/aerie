/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

export interface RavenExportResourceSubBand {
  addTo: boolean;
  autoScale: boolean;
  color: string;
  decimate: boolean;
  fill: boolean;
  fillColor: string;
  height: number;
  heightPadding: number;
  icon: string;
  interpolation: string;
  isDuration: boolean;
  isTime: boolean;
  label: string;
  labelColor: string | number[];
  labelFont: string;
  labelPin: string;
  labelUnit: string;
  logTicks: boolean;
  maxLimit?: number;
  maxTimeRange: {
    end: number;
    start: number;
  };
  minLimit?: number;
  name: string;
  points: never[]; // empty array
  scientificNotation: boolean;
  showIcon: boolean;
  showLabelPin: boolean;
  showLabelUnit: boolean;
  showTooltip: boolean;
  sourceIds: string[];
  tableColumns: any[];
  type: string;
}
