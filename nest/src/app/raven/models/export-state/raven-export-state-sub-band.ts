/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

export interface RavenExportStateSubBand {
  alignLabel: number;
  addTo: boolean;
  baselineLabel: number;
  borderWidth: number;

  // Use in line plot only.
  color: string;

  editable: boolean;

  // Use in line plot only.
  fill: boolean;
  fillColor: string;

  height: number;
  heightPadding: number;

  // Use in line plot only.
  icon: string;

  isNumeric: boolean;
  label: string;
  labelColor: string | number[];
  labelFont: string;
  labelPin: string;
  maxTimeRange: {
    end: number;
    start: number;
  };
  name: string;
  points: never[]; // empty array
  possibleStates: string[];

  // Use in line plot only.
  showIcon: boolean;

  showLabelPin: boolean;
  showStateChangeTimes: boolean;
  showTooltip: boolean;
  sourceIds: string[];
  stateLabelFontSize: number;
  tableColumns: any[];
  timeDelta?: number;
  type: string;
}
