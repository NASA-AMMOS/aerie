/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { utc as momentUtc } from 'moment';
import { tz as momentTz } from 'moment-timezone';
import { RavenEpoch, RavenEpochTime } from '../../raven/models';
import { SituationalAwarenessState } from '../../raven/reducers/situational-awareness.reducer';

/**
 * Converts a JS Date object to a timestring.
 * Called `toTimeString` in Raven1.
 */
export function dateToTimestring(
  date: Date,
  showMilliseconds: boolean,
): string {
  if (date.getUTCFullYear() === 1970) {
    return '0';
  }

  const year = date
    .getUTCFullYear()
    .toString()
    .padStart(4, '0');
  const doy = getDOY(date)
    .toString()
    .padStart(3, '0');
  const hour = date
    .getUTCHours()
    .toString()
    .padStart(2, '0');
  const min = date
    .getUTCMinutes()
    .toString()
    .padStart(2, '0');
  const sec = date
    .getUTCSeconds()
    .toString()
    .padStart(2, '0');

  let timeString = '';

  if (!showMilliseconds) {
    timeString = year + '-' + doy + 'T' + hour + ':' + min + ':' + sec;
  } else {
    timeString =
      year +
      '-' +
      doy +
      'T' +
      hour +
      ':' +
      min +
      ':' +
      sec +
      '.' +
      date
        .getUTCMilliseconds()
        .toString()
        .padStart(2, '0');
  }

  return timeString;
}

/**
 * Converts a datetime string (2019-04-10T18:02:59.000Z) to epoch (1554919379000)
 */
export function datetimeToEpoch(datetime: Date) {
  return datetime.getTime() / 1000;
}

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
    remainder %= 24 * 60 * 60;
  }
  if (remainder !== 0) {
    hrs = Math.floor(remainder / (60 * 60));
    remainder %= 60 * 60;
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
 * Takes a duration string of the form `20s` and returns a time.
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
 * Takes a duration string of the form `12:00:00` and returns a corresponding time.
 * Previously called `fromDurationString` in Raven1.
 */
export function fromDuration(duration: string): number {
  const results = duration.match(
    new RegExp(/([+|-]?)(\d+T)?(\d{2}):(\d{2}):(\d{2})(\.\d{3})?/),
  );

  if (results) {
    let day = 0;

    if (results[2]) {
      day = parseInt(results[2].replace('T', ''), 10) * 24 * 60 * 60;
    }

    const hour = parseInt(results[3], 10) * 60 * 60;
    const min = parseInt(results[4], 10) * 60;
    const sec = parseInt(results[5], 10);

    let msec = 0;
    if (results[6]) {
      msec = parseInt(results[6].replace('.', ''), 10) / 1000;
    }

    if (results[1] && results[1] === '-') {
      return -(day + hour + min + sec + msec);
    } else {
      return day + hour + min + sec + msec;
    }
  }

  return 0;
}

/**
 * Returns a default day code if `dayCode` is the empty string.
 */
export function formatDayCode(dayCode: string): string {
  return dayCode === '' ? ' days ' : dayCode;
}

/**
 * Formats an epoch time to a duration string.
 */
export function formatEpochDuration(
  epochTime: RavenEpochTime,
  dayCode: string,
): string {
  const day = formatDayCode(dayCode);
  const epochStr = epochTime.epoch > 0 ? epochTime.epoch + day : '';
  const hourStr = epochTime.hours > 0 ? epochTime.hours + 'h' : '';

  return (
    epochStr +
    hourStr +
    epochTime.minutes +
    'm' +
    epochTime.seconds +
    's' +
    epochTime.milliseconds +
    'ms'
  );
}

/**
 * Formats an epoch time to a string.
 */
export function formatEpochTime(
  epochTime: RavenEpochTime,
  dayCode: string,
  includeMilli: boolean,
): string {
  const day = formatDayCode(dayCode);
  const hours = epochTime.hours.toString().padStart(2, '0');
  const minutes = epochTime.minutes.toString().padStart(2, '0');
  const seconds = epochTime.seconds.toString().padStart(2, '0');
  const milliseconds = epochTime.milliseconds.toString().padStart(3, '0');

  return (
    epochTime.sign +
    Math.abs(epochTime.epoch) +
    day +
    `${hours}:${minutes}:${seconds}${includeMilli ? '.' + milliseconds : ''}`
  );
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
  const difference = toEpochTime(
    end - start + landingSeconds,
    earthSecPerEpochSec,
    epoch,
  );

  const epochDuration: RavenEpochTime = {
    epoch: difference.epoch,
    hours: difference.hours,
    milliseconds: difference.milliseconds,
    minutes: difference.minutes,
    seconds: difference.seconds,
    sign: '',
  };

  return (
    formatEpochTime(startEpoch, dayCode, true) +
    ' to ' +
    formatEpochTime(endEpoch, dayCode, true) +
    ' (' +
    formatEpochDuration(epochDuration, dayCode) +
    ')'
  );
}

/**
 * Formatter function used to format the Raven Time Band.
 */
export function formatTimeTickTFormat(
  obj: any,
  epoch: RavenEpoch | null,
  earthSecPerEpochSec: number,
  dayCode: string,
): any[] {
  const time = obj.time;
  const band = obj.timeBand;

  const tickYPosition = band.height / 4;
  const formattedTimes = [];
  const formatDecrementer = 10;

  formattedTimes.push({
    formattedTime: timestamp(time, false),
    y: tickYPosition,
  });

  if (epoch) {
    const epochTime = toEpochTime(time, earthSecPerEpochSec, epoch);
    const formattedEpoch = formatEpochTime(epochTime, dayCode, false);

    formattedTimes.push({
      formattedTime: formattedEpoch,
      y: tickYPosition + formatDecrementer,
    });
  } else {
    formattedTimes.push({
      formattedTime: timestampYMD(time),
      y: tickYPosition + formatDecrementer,
    });
  }

  return formattedTimes;
}

/**
 * Get day of year on a Date.
 * Taken from ctl.js to lessen dependency on the library.
 */
export function getDOY(date: Date): number {
  const d = Date.UTC(date.getUTCFullYear(), 0, 0);
  return Math.floor((date.getTime() - d) / 8.64e7);
}

/**
 * Returns start and end time range for the initial page.
 * `pageDuration` is defaulted to 1 day.
 */
export function getInitialPageStartEndTime(
  situationalAwareness: SituationalAwarenessState,
) {
  let start = 0;
  let pageDuration = 24 * 60 * 60;
  if (situationalAwareness.useNow) {
    start = situationalAwareness.nowMinus
      ? new Date().getTime() / 1000 - situationalAwareness.nowMinus
      : new Date().getTime() / 1000;
    if (
      situationalAwareness.nowMinus &&
      situationalAwareness.nowPlus &&
      situationalAwareness.nowMinus + situationalAwareness.nowPlus !== 0
    ) {
      pageDuration =
        situationalAwareness.nowMinus + situationalAwareness.nowPlus;
    }
  } else {
    start = situationalAwareness.startTime
      ? situationalAwareness.startTime
      : new Date().getTime() / 1000;
    if (
      situationalAwareness.pageDuration &&
      situationalAwareness.pageDuration !== 0
    ) {
      pageDuration = situationalAwareness.pageDuration;
    }
  }
  return { start, end: start + pageDuration };
}

/**
 * Return name for local timezone.
 */
export function getLocalTimezoneName() {
  const zoneName = momentTz.guess();
  return momentTz(zoneName).zoneAbbr();
}

/**
 * Recalculates the max time-range from a list of values with start and end number props.
 */
export function getMaxTimeRange(arr: any[]) {
  let maxTime = Number.MIN_SAFE_INTEGER;
  let minTime = Number.MAX_SAFE_INTEGER;

  for (let i = 0, l = arr.length; i < l; ++i) {
    const value = arr[i];
    if (value.pointStatus !== 'deleted') {
      const start = value.start;
      let end = value.start;

      if (value.end) {
        end = value.end;
      }

      if (!value.duration) {
        end = start;
      }

      if (start < minTime) {
        minTime = start;
      }
      if (end > maxTime) {
        maxTime = end;
      }
    }
  }

  return {
    end: maxTime,
    start: minTime,
  };
}

/**
 * Return situationalAwareness startTime. If 'now' is used, startTime is now - nowMinus.
 */
export function getSituationalAwarenessStartTime(
  situationalAwareness: SituationalAwarenessState,
): string {
  if (situationalAwareness.useNow) {
    const start = situationalAwareness.nowMinus
      ? new Date().getTime() / 1000 - situationalAwareness.nowMinus
      : new Date().getTime() / 1000;
    return timestamp(start);
  } else {
    return situationalAwareness.startTime
      ? timestamp(situationalAwareness.startTime)
      : timestamp(new Date().getTime() / 1000);
  }
}

/**
 * Return situationAwareness pageDuration.
 */
export function getSituationalAwarenessPageDuration(
  situationalAwareness: SituationalAwarenessState,
): string {
  if (situationalAwareness.useNow) {
    return situationalAwareness.nowMinus && situationalAwareness.nowPlus
      ? toDuration(
          (situationalAwareness.nowMinus + situationalAwareness.nowPlus) * 1000,
          false,
        )
      : '001T00:00:00';
  } else {
    return situationalAwareness.pageDuration &&
      situationalAwareness.pageDuration !== 0
      ? toDuration(situationalAwareness.pageDuration * 1000, false)
      : '001T00:00:00';
  }
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
 * Returns a YMD local timestamp (e.g. 2023-06-13 00:00:00) based on a time.
 */
export function timestampYMD(time: number) {
  return momentUtc(time * 1000)
    .local()
    .format('YYYY-MM-DD HH:mm:ss');
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
 * Converts a time to a duration string (e.g. 12:00:00).
 * This is the inverse of `fromDuration`.
 * Previously called `toDurationString` in Raven1.
 */
export function toDuration(msecs: number, showMilliseconds: boolean): string {
  let negative = false;

  if (msecs < 0) {
    negative = true;
    msecs = -msecs;
  }

  let seconds = msecs / 1000;

  const doy = Math.floor(seconds / (60 * 60 * 24))
    .toString()
    .padStart(3, '0');
  seconds -= parseInt(doy, 10) * (60 * 60 * 24);

  const hour = Math.floor(seconds / (60 * 60))
    .toString()
    .padStart(2, '0');
  seconds -= parseInt(hour, 10) * (60 * 60);

  const min = Math.floor(seconds / 60)
    .toString()
    .padStart(2, '0');
  seconds -= parseInt(min, 10) * 60;

  const sec = Math.floor(seconds)
    .toString()
    .padStart(2, '0');

  let timeString = '';

  if (!showMilliseconds) {
    timeString = doy + 'T' + hour + ':' + min + ':' + sec;
  } else {
    seconds -= parseInt(sec, 10);
    timeString =
      doy +
      'T' +
      hour +
      ':' +
      min +
      ':' +
      sec +
      '.' +
      Math.floor(seconds * 1000)
        .toString()
        .padStart(3, '0');
  }

  if (negative) {
    timeString = `-${timeString}`;
  }

  return timeString;
}

/**
 * Converts SCET seconds to an EpochTime.
 */
export function toEpochTime(
  scetSeconds: number,
  earthSecPerEpochSec: number,
  epoch: RavenEpoch | null,
): RavenEpochTime {
  let landingSeconds = 0;

  if (epoch !== null) {
    landingSeconds = momentUtc(epoch.value).unix();
  }

  const epochTime =
    (scetSeconds - landingSeconds) / (60 * 60 * 24 * earthSecPerEpochSec);
  const remainder = Math.abs(epochTime) % 1;
  const ms = remainder * 60 * 60 * 24 * 1000;

  return {
    epoch: epochTime < 0 ? Math.ceil(epochTime) : Math.floor(epochTime),
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
export function utc(time: string | number): number {
  if (typeof time === 'string') {
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
  return 0;
}
