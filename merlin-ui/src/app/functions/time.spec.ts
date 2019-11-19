import { getDoy, getDoyTimestamp, getUnixEpochTime } from './time';

describe('time', () => {
  describe('getDoy', () => {
    it('should properly calculate the first day of the year', () => {
      const doy = getDoy(new Date('1/1/2019'));
      expect(doy).toEqual(1);
    });

    it('should properly calculate the last day of the year', () => {
      const doy = getDoy(new Date('12/31/2019'));
      expect(doy).toEqual(365);
    });
  });

  describe('getDoyTimestamp', () => {
    it('should properly calculate the first day of the year', () => {
      const time = new Date('1/1/2019').getTime() / 1000;
      const doyTimestamp = getDoyTimestamp(time);
      expect(doyTimestamp).toEqual('2019-001T08:00:00.000');
    });

    it('should properly calculate the last day of the year', () => {
      const time = new Date('12/31/2019').getTime() / 1000;
      const doyTimestamp = getDoyTimestamp(time);
      expect(doyTimestamp).toEqual('2019-365T08:00:00.000');
    });
  });

  describe('getUnixEpochTime', () => {
    it('should properly calculate the first day of the year', () => {
      const time = new Date('1/1/2019').getTime() / 1000;
      const unixEpochTime = getUnixEpochTime('2019-001T08:00:00.000');
      expect(unixEpochTime).toEqual(time);
    });

    it('should properly calculate the last day of the year', () => {
      const time = new Date('12/31/2019').getTime() / 1000;
      const unixEpochTime = getUnixEpochTime('2019-365T08:00:00.000');
      expect(unixEpochTime).toEqual(time);
    });
  });
});
