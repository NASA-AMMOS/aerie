export function getIntervalFromDoyRange(startTime: string, endTime: string): string {
  const startTimeMs = getUnixEpochTime(startTime);
  const endTimeMs = getUnixEpochTime(endTime);
  const differenceMs = endTimeMs - startTimeMs;
  const seconds = Math.floor(differenceMs / 1000);
  const milliseconds = differenceMs % 1000;
  return `${seconds} seconds ${milliseconds} milliseconds`;
}

export function getUnixEpochTime(doyTimestamp: string): number {
  const re = /(\d{4})-(\d{3})T(\d{2}):(\d{2}):(\d{2})\.?(\d{3})?/;
  const match = re.exec(doyTimestamp);

  if (match) {
    const [, year, doy, hours, mins, secs, msecs = '0'] = match;
    return Date.UTC(+year, 0, +doy, +hours, +mins, +secs, +msecs);
  }

  return 0;
}
