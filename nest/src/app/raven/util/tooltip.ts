/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { RavenEpoch } from '../models';
import { formatEpochTimeRange, formatTimeRangeTFormat } from '../util/time';

/**
 * Linearly interpolate x3 between two points (x1, y1) and (x2, y2).
 * Taken from ctl.js to lessen dependency on the library.
 */
export function interpolateY3(
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  x3: number,
): number {
  if (x1 === x2) {
    return y1;
  } else {
    return y2 - (x2 - x3) * ((y2 - y1) / (x2 - x1));
  }
}

/**
 * Helper that gets tooltip text for a CTL interpolated interval.
 */
export function getInterpolatedTooltipText(
  obj: any,
  earthSecPerEpochSec: number,
  epoch: RavenEpoch | null,
  dayCode: string,
) {
  const { band, interval, time } = obj;

  const timeInterval = formatTimeRangeTFormat(interval.start, interval.end);
  const epochInterval = formatEpochTimeRange(
    interval.start,
    interval.end,
    null,
    earthSecPerEpochSec,
    dayCode,
  );
  let valueAtTime = interpolateY3(
    interval.start,
    interval.startValue,
    interval.end,
    interval.endValue,
    time,
  );
  valueAtTime = band.onFormatTickValue
    ? band.onFormatTickValue(valueAtTime, true)
    : valueAtTime;
  const startValue = band.onFormatTickValue
    ? band.onFormatTickValue(interval.startValue, true)
    : interval.startValue;
  const endValue = band.onFormatTickValue
    ? band.onFormatTickValue(interval.endValue, true)
    : interval.endValue;

  return `
    <table>
      <tr>
        <td>
          <strong>Resource:</strong>
        </td>
        <td>
          ${band.label}
        </td>
      </tr>
      <tr>
        <td>
          <strong>SCET Interval:</strong>
        </td>
        <td>
          ${timeInterval}
        </td>
      </tr>
      ${
        epoch
          ? `
      <tr>
        <td>
          <strong>Epoch Relative:</strong>
        </td>
        <td>
          ${epochInterval}
        </td>
      </tr>`
          : ''
      }
      <tr>
        <td>
          <strong>Start Value:</strong>
        </td>
        <td>
          ${startValue}
        </td>
      </tr>
      <tr>
        <td>
          <strong>End Value:</strong>
        </td>
        <td>
          ${endValue}
        </td>
      </tr>
      <tr>
        <td>
          <strong>Value:</strong>
        </td>
        <td>
          ${valueAtTime}
        </td>
      </tr>
    </table>
  `;
}

/**
 * Helper that gets generic tooltip text based on interval and band parameters.
 */
export function getTooltipText(
  obj: any,
  earthSecPerEpochSec: number,
  epoch: RavenEpoch | null,
  dayCode: string,
) {
  const { interval, band } = obj;

  const timeInterval = formatTimeRangeTFormat(interval.start, interval.end);
  const epochInterval = formatEpochTimeRange(
    interval.start,
    interval.end,
    epoch,
    earthSecPerEpochSec,
    dayCode,
  );
  const value = band.onFormatTickValue
    ? band.onFormatTickValue(interval.properties.Value, true)
    : interval.properties.Value;

  let parameterString = '';
  if (interval.parameters && interval.parameters.length > 0) {
    parameterString = '<p><h3>Parameters:</h3><table>';
    for (let i = 0, l = interval.parameters.length; i < l; ++i) {
      const param = interval.parameters[i];
      parameterString =
        parameterString +
        `
        <tr>
         <td>
          <strong>${param.Name}</strong>
          </td>
          <td>
            ${param.Value}
          </td>
        </tr>
      `;
    }
    parameterString = parameterString + '</table></p>';
  }

  return `
    <table>
      <tr>
        <td>
          <strong>Resource:</strong>
        </td>
        <td>
          ${band.label}
        </td>
      </tr>
      <tr>
        <td>
          <strong>Interval:</strong>
        </td>
        <td>
          ${timeInterval}
        </td>
      </tr>
      ${
        epoch !== null
          ? `
      <tr>
        <td>
          <strong>Epoch Relative:</strong>
        </td>
        <td>
          ${epochInterval}
        </td>
      </tr>`
          : ''
      }
      <tr>
        <td>
          <strong>Value:</strong>
        </td>
        <td>
          ${value ||
            interval.properties.text ||
            interval.label ||
            interval.properties.Value}
        </td>
      </tr>
    </table>
  ${parameterString}`;
}
