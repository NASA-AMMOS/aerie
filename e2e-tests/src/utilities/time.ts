import parse from 'postgres-interval';

const time = {
  /**
   * Returns a Postgres Interval over the specified range of DOY strings.
   */
  getIntervalFromDoyRange(startTime: string, endTime: string): string {
    const startTimeMs = time.getUnixEpochTime(startTime);
    const endTimeMs = time.getUnixEpochTime(endTime);
    const differenceMs = endTimeMs - startTimeMs;
    const seconds = Math.floor(differenceMs / 1000);
    const milliseconds = differenceMs % 1000;
    return `${seconds} seconds ${milliseconds} milliseconds`;
  },

  /**
   * Get a unix epoch time in milliseconds given a day-of-year timestamp.
   * @example getUnixEpochTime('2019-365T08:00:00.000') -> 1577779200000
   * @note inverse of getDoyTime
   */
  getUnixEpochTime(doyTimestamp: string): number {
    const re = /(\d{4})-(\d{3})T(\d{2}):(\d{2}):(\d{2})\.?(\d{3})?/;
    const match = re.exec(doyTimestamp);

    if (match) {
      const [, year, doy, hours, mins, secs, msecs = '0'] = match;
      return Date.UTC(+year, 0, +doy, +hours, +mins, +secs, +msecs);
    }

    return 0;
  },

  /**
   * Get the day-of-year for a given date.
   * @example getDoy(new Date('1/3/2019')) -> 3
   * @see https://stackoverflow.com/a/8619946
   */
  getDoy(date: Date): number {
    const start = Date.UTC(date.getUTCFullYear(), 0, 0);
    const diff = date.getTime() - start;
    const oneDay = 8.64e7; // Number of milliseconds in a day.
    return Math.floor(diff / oneDay);
  },

  /**
   * Get a day-of-year timestamp from a given JavaScript Date object.
   * @example getDoyTime(new Date(1577779200000)) -> 2019-365T08:00:00.000
   * @note inverse of getUnixEpochTime
   */
   getDoyTime(date: Date, includeMsecs = true): string {
    const year = date.getUTCFullYear();
    const doy = time.getDoy(date).toString().padStart(3, '0');
    const hours = date.getUTCHours().toString().padStart(2, '0');
    const mins = date.getUTCMinutes().toString().padStart(2, '0');
    const secs = date.getUTCSeconds().toString().padStart(2, '0');
    const msecs = date.getUTCMilliseconds().toString().padStart(3, '0');

    let doyTimestamp = `${year}-${doy}T${hours}:${mins}:${secs}`;

    if (includeMsecs) {
      doyTimestamp += `.${msecs}`;
    }

    return doyTimestamp;
  },

  /**
   * Get a day-of-year timestamp from a given JavaScript Date object, and
   * a duration in Postgres Interval format.
   */
    getDoyTimeFromDuration(startDate: Date, duration: string, includeMsecs = true): string {
      const interval = parse(duration);
      const { hours, milliseconds, minutes, seconds } = interval;
      const endDate = new Date(startDate.getTime());
      endDate.setUTCHours(endDate.getUTCHours() + hours);
      endDate.setUTCMinutes(endDate.getUTCMinutes() + minutes);
      endDate.setUTCSeconds(endDate.getUTCSeconds() + seconds);
      endDate.setUTCMilliseconds(endDate.getUTCMilliseconds() + milliseconds);
      return time.getDoyTime(endDate, includeMsecs);
  },
};


export default time;
