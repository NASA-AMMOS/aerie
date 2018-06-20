/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

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
 * Get day of year on a Date.
 * Taken from ctl.js to lessen dependency on the library.
 */
export function getDOY(date: Date): number {
  const d = Date.UTC(date.getUTCFullYear(), 0, 0);
  return Math.floor((date.getTime() - d) / 8.64e+7);
}

/**
 * Get a timestamp string from a time.
 */
export function timestamp(time: number): string {
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

  const msecs = date.getUTCMilliseconds().toString();
  timeStr += `.${msecs.padStart(3, '0')}`;

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
