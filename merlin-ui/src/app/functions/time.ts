/**
 * Get the day-of-year for a given date.
 * @example getDoy(new Date('1/3/2019')) -> 3
 * @see https://stackoverflow.com/a/8619946
 */
export function getDoy(date: Date): number {
  const start = Date.UTC(date.getUTCFullYear(), 0, 0);
  const diff = date.getTime() - start;
  const oneDay = 8.64e7; // Number of milliseconds in a day.
  return Math.floor(diff / oneDay);
}

/**
 * Get a day-of-year timestamp from a given unix epoch time in seconds.
 * @example getDoyTimestamp(1577779200) -> 2019-365T08:00:00.000
 * @note inverse of getUnixEpochTime
 */
export function getDoyTimestamp(
  unixEpochTime: number,
  includeMsecs: boolean = true,
): string {
  const date = new Date(unixEpochTime * 1000);
  const year = date.getUTCFullYear();
  const doy = getDoy(date)
    .toString()
    .padStart(3, '0');
  const hours = date
    .getUTCHours()
    .toString()
    .padStart(2, '0');
  const mins = date
    .getUTCMinutes()
    .toString()
    .padStart(2, '0');
  const secs = date
    .getUTCSeconds()
    .toString()
    .padStart(2, '0');
  const msecs = date
    .getUTCMilliseconds()
    .toString()
    .padStart(3, '0');

  let doyTimestamp = `${year}-${doy}T${hours}:${mins}:${secs}`;

  if (includeMsecs) {
    doyTimestamp += `.${msecs}`;
  }

  return doyTimestamp;
}

/**
 * Get a unix epoch time in seconds given a day-of-year timestamp.
 * @example getUnixEpochTime('2019-365T08:00:00.000') -> 1577779200
 * @note inverse of getDoyTimestamp
 */
export function getUnixEpochTime(doyTimestamp: string): number {
  const re = /(\d{4})-(\d{3})T(\d{2}):(\d{2}):(\d{2})\.?(\d{3})?/;
  const match = re.exec(doyTimestamp);

  if (match) {
    const [, year, doy, hours, mins, secs, msecs = '0'] = match;
    return Date.UTC(+year, 0, +doy, +hours, +mins, +secs, +msecs) / 1000;
  }

  return 0;
}
