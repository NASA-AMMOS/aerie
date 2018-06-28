/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { utc as momentUtc } from 'moment';

import {
  RavenEpoch,
  RavenEpochTime,
} from './../models';

/**
 * Converts a duration to a DHMS (days, hours, minutes, seconds) string.
 */
export function dhms(duration: number): string {
  if (duration === 0) {
    return '0m';
  }

  const negative = duration < 0;

  if (negative) {
    duration = Math.abs(duration);
  }

  let days = 0;
  let hrs = 0;
  let mins = 0;
  let secs = 0;
  let msecs = 0;
  let remainder = duration;

  if (remainder !== 0) {
    days = Math.floor(remainder / (24 * 60 * 60));
    remainder %= (24 * 60 * 60);
  }
  if (remainder !== 0) {
    hrs = Math.floor(remainder / (60 * 60));
    remainder %= (60 * 60);
  }
  if (remainder !== 0) {
    mins = Math.floor(remainder / 60);
    remainder %= 60;
  }
  if (remainder !== 0) {
    secs = Math.floor(remainder);
    remainder -= secs;
  }
  msecs = Math.floor(remainder * 1000);

  let durationStr = negative ? '-' : '';

  if (days !== 0) {
    durationStr += `${days}d`;
  }
  if (hrs !== 0) {
    durationStr += `${hrs}h`;
  }
  if (mins !== 0) {
    durationStr += `${mins}m`;
  }
  if (secs !== 0) {
    durationStr += `${secs}s`;
  }
  durationStr += `${msecs}ms`;

  return durationStr;
}

/**
 * Formats a start and end time into a time range string.
 */
export function formatTimeRangeTFormat(start: number, end: number): string {
  const startStr = timestamp(start);
  const endStr = timestamp(end);
  const durStr = dhms(end - start);

  return startStr + ' - ' + endStr + ' (' + durStr + ')';
}

/**
 * Takes a duration string (e.g. 20s) and returns a time.
 */
export function fromDHMString(duration: string): number {
  if (!duration) {
    return 0;
  }

  const re = /^((\d+)[d,D])?((\d+)[h,H])?((\d+)[m,M])?((\d+)[s,S])?((\d+)ms)?$/;
  const results = duration.match(re);

  if (!results) {
    return 0;
  }

  let days = 0;
  let hours = 0;
  let mins = 0;
  let secs = 0;
  let msecs = 0;

  if (results[2] !== undefined) {
    days = parseInt(results[1], 10);
  }
  if (results[4] !== undefined) {
    hours = parseInt(results[3], 10);
  }
  if (results[6] !== undefined) {
    mins = parseInt(results[5], 10);
  }
  if (results[8] !== undefined) {
    secs = parseInt(results[7], 10);
  }
  if (results[10] !== undefined) {
    msecs = parseInt(results[9], 10);
  }

  return days * 24 * 3600 + hours * 3600 + mins * 60 + secs + msecs / 1000;
}

/**
 * Formats an epoch time to a duration string.
 */
export function formatEpochDuration(epochTime: RavenEpochTime, dayCode: string): string {
  const epochStr = epochTime.epoch > 0 ? (epochTime.epoch + dayCode) : '';
  const hourStr = epochTime.hours > 0 ? (epochTime.hours + 'h') : '';

  return epochStr + hourStr + epochTime.minutes + 'm' + epochTime.seconds + 's' + epochTime.milliseconds + 'ms';
}

/**
 * Formats an epoch time to a string.
 */
export function formatEpochTime(epochTime: RavenEpochTime, dayCode: string): string {
  const hours = epochTime.hours.toString().padStart(2, '0');
  const minutes = epochTime.minutes.toString().padStart(2, '0');
  const seconds = epochTime.seconds.toString().padStart(2, '0');
  const milliseconds = epochTime.milliseconds.toString().padStart(3, '0');

  return epochTime.sign + Math.abs(epochTime.epoch) + dayCode + `${hours}:${minutes}:${seconds}.${milliseconds}`;
}

/**
 * Formats a start and end time to an epoch time range string.
 */
export function formatEpochTimeRange(
  start: number,
  end: number,
  epoch: RavenEpoch | null,
  earthSecPerEpochSec: number,
  dayCode: string,
): string {
  let landingSeconds = 0;

  if (epoch !== null) {
    landingSeconds = momentUtc(epoch.value).unix();
  }

  const startEpoch = toEpochTime(start, earthSecPerEpochSec, epoch);
  const endEpoch = toEpochTime(end, earthSecPerEpochSec, epoch);
  const difference = toEpochTime(end - start + landingSeconds, earthSecPerEpochSec, epoch);

  const epochDuration: RavenEpochTime = {
    epoch: difference.epoch,
    hours: difference.hours,
    milliseconds: difference.milliseconds,
    minutes: difference.minutes,
    seconds: difference.seconds,
    sign: '',
  };

  return formatEpochTime(startEpoch, dayCode) + ' to ' + formatEpochTime(endEpoch, dayCode) + ' (' + formatEpochDuration(epochDuration, dayCode) + ')';
}

/**
 * Formatter function used to format the Raven Time Band.
 */
export function formatTimeTickTFormat(
  obj: any,
  epoch: RavenEpoch |  null,
  earthSecPerEpochSec: number,
  dayCode: string,
): any[] {
  const time = obj.time;
  const band = obj.timeBand;

  const formattedTimes = [];
  let formatDecrementer = 5;

  formattedTimes.push({
    formattedTime: timestamp(time, false),
    y: band.height / 2,
  });

  if (epoch) {
    const epochTime = toEpochTime(time, earthSecPerEpochSec, epoch);
    const formattedEpoch = formatEpochTime(epochTime, dayCode);

    formattedTimes.push({
      formattedTime: formattedEpoch,
      y: band.height - formatDecrementer,
    });

    formatDecrementer -= 5;
  } else {
    formattedTimes.push({
      formattedTime: timestampYMD(time),
      y: band.height - formatDecrementer,
    });

    formatDecrementer -= 5;
  }

  return formattedTimes;
}

/**
 * Get day of year on a Date.
 * Taken from ctl.js to lessen dependency on the library.
 */
export function getDOY(date: Date): number {
  const d = Date.UTC(date.getUTCFullYear(), 0, 0);
  return Math.floor((date.getTime() - d) / 8.64e+7);
}

/**
 * Get a DOY timestamp (e.g. 2023-164T00:00:00.000) from a time.
 * Includes milliseconds by default.
 * In Raven1 this was called `formatTimeTFormat`.
 */
export function timestamp(time: number, includeMsecs: boolean = true): string {
  const date = new Date(time * 1000);
  const year = date.getUTCFullYear();
  const doy = getDOY(date).toString();
  const hour = date.getUTCHours().toString();
  const mins = date.getUTCMinutes().toString();

  let timeStr = '';
  timeStr = `${year}-${doy.padStart(3, '0')}T`;
  timeStr += `${hour.padStart(2, '0')}:${mins.padStart(2, '0')}`;

  const secs = date.getUTCSeconds().toString();
  timeStr += `:${secs.padStart(2, '0')}`;

  if (includeMsecs) {
    const msecs = date.getUTCMilliseconds().toString();
    timeStr += `.${msecs.padStart(3, '0')}`;
  }

  return timeStr;
}

/**
 * Returns a YMD timestamp (e.g. 2023-06-13 00:00:00) based on a time.
 */
export function timestampYMD(time: number) {
  const date = new Date(time * 1000);
  const year = date.getUTCFullYear();
  const mon = date.getUTCMonth() + 1;
  const day = date.getUTCDate();
  const hour = date.getUTCHours();
  const mins = date.getUTCMinutes();

  let timeStr = '';
  timeStr = year + '-' + mon.toString().padStart(2, '0') + '-' + day.toString().padStart(2, '0') + ' ';
  timeStr += hour.toString().padStart(2, '0') + ':' + mins.toString().padStart(2, '0');

  const secs = date.getUTCSeconds();
  timeStr += ':' + secs.toString().padStart(2, '0');

  return timeStr;
}

/**
 * Formats a start and end time into a time range string.
 */
export function timestring(start: number, end: number): string {
  const startStr = timestamp(start);
  const endStr = timestamp(end);
  const durStr = dhms(end - start);

  return `${startStr} - ${endStr} (${durStr})`;
}

/**
 * Converts SCET seconds to an EpochTime.
 */
export function toEpochTime(scetSeconds: number, earthSecPerEpochSec: number, epoch: RavenEpoch | null): RavenEpochTime {
  let landingSeconds = 0;

  if (epoch !== null) {
    landingSeconds = momentUtc(epoch.value).unix();
  }

  const epochTime = (scetSeconds - landingSeconds) / (60 * 60 * 24 * earthSecPerEpochSec);
  const remainder = Math.abs(epochTime) % 1;
  const ms = remainder * 60 * 60 * 24 * 1000;

  return {
    epoch: epochTime < 0 ? Math.ceil (epochTime) : Math.floor(epochTime),
    hours: momentUtc(ms).hour(),
    milliseconds: momentUtc(ms).millisecond(),
    minutes: momentUtc(ms).minute(),
    seconds: momentUtc(ms).second(),
    sign: epochTime < 0 ? '-' : '+',
  };
}

/**
 * Takes a ISO 8601 time string and returns an epoch number representation of that time.
 */
export function utc(time: string): number {
  const re = /(\d\d\d\d)-(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?/;
  const results = time.match(re);
  let withMilliseconds = true;

  if (!results) {
    return 0;
  }

  if (!results[6]) {
    withMilliseconds = false;
  }

  const year = parseInt(results[1], 10);
  const doy = parseInt(results[2], 10);
  const hour = parseInt(results[3], 10);
  const min = parseInt(results[4], 10);
  const sec = parseInt(results[5], 10);
  const msec = withMilliseconds ? parseInt(results[6], 10) : 0;
  const msecsUTC = Date.UTC(year, 0, doy, hour, min, sec, msec);

  return msecsUTC / 1000;
}
