/*
  global _, d3
*/

/**
 * Class with static utility methods for use in Falcon.
 *
 * @polymer
 * @mixinFunction
 */
class FalconUtils {
  /**
   * Takes an array of `activity` timelineData and converts it to points
   * for drawing in Falcon.
   *
   * @static
   * @param {Array} timelineData
   * @returns
   *
   * @memberof FalconUtils
   */
  static activityToPoints(timelineData) {
    const points = [];

    timelineData.forEach((data) => {
      const activityId = data['Activity ID'];
      const activityName = data['Activity Name'];
      const activityParameters = data['Activity Parameters'];
      const activityType = data['Activity Type'];
      const ancestors = data.ancestors;
      const childrenUrl = data.childrenUrl;
      const color = this.getColorFromMetadata(data.Metadata);
      const descendantsUrl = data.descendantsUrl;
      const endTimestamp = data['Tend Assigned'];
      const id = data.__document_id;
      const startTimestamp = data['Tstart Assigned'];
      const uniqueId = _.uniqueId();

      const start = this.utc(startTimestamp);
      const end = this.utc(endTimestamp);
      const duration = end - start;

      let hasLegend = false;
      let legend = '';
      if (data.Metadata) {
        data.Metadata.forEach((d) => {
          if (d.Name === 'legend') {
            legend = d.Value;
            hasLegend = true;
          }
        });
      }

      points.push({
        activityId,
        activityName,
        activityParameters,
        activityType,
        ancestors,
        childrenUrl,
        color,
        descendantsUrl,
        duration,
        end,
        endTimestamp,
        hasLegend,
        id,
        legend,
        start,
        startTimestamp,
        uniqueId,
      });
    });

    return points;
  }

  /**
   * Get ticks based on a scale type.
   *
   * @static
   * @param {String} type
   * @param {Number} min
   * @param {Number} max
   * @param {Number} height
   * @returns
   * @memberof FalconUtils
   */
  static ticks(type, min, max, height) {
    if (type === 'log') {
      // TODO.
      return [];
    }
    return FalconUtils.linear(min, max, height);
  }

  /**
   * Get a list of linear ticks based on a min, max and height.
   * Height is the height of the canvas.
   *
   * @static
   * @param {Number} min
   * @param {Number} max
   * @param {Number} height
   * @returns
   * @memberof FalconUtils
   */
  static linear(min, max, height) {
    // Typical linear scale with a domain of [min, max].
    const scale = d3.scaleLinear().domain([min, max]);

    // Number of ticks is floor(height / 20).
    // The 20 is sort-of arbitrary, and was picked mainly because it looks good.
    const ticks = scale.ticks(Math.floor(height / 20));

    // For each tick, if val > 10,000 or < -10,0000 then use exponential notation.
    // Otherwise use fixed notation.
    // If using fixed notation and the value is divisible by 1, use 0 decimal precision. Otherwise use 6 decimal precision.
    return ticks.map((val) => {
      if (val > 10000 || val < -10000) {
        return val.toExponential(3);
      }
      return val.toFixed(val % 1 === 0 ? 0 : 6);
    });
  }

  /**
   * Converts a duration to a DHMS (days, hours, minutes, seconds) string.
   *
   * @static
   * @param {any} duration
   * @memberof FalconUtils
   */
  static dhms(duration) {
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
   * Helper that gets a color from metadata.
   *
   * @static
   * @param {any} metadata
   * @returns
   *
   * @memberof FalconUtils
   */
  static getColorFromMetadata(metadata) {
    let color = [0, 0, 0];

    metadata.forEach((data) => {
      if (data.Name.toLowerCase() === 'color') {
        color = FalconUtils.getColor(data.Value);
      }
    });

    return color;
  }

  /**
   * Takes a string of a color name and returns
   * an RGB representation of that color.
   *
   * @static
   * @param {String} color
   * @returns {Array}
   *
   * @memberof FalconUtils
   */
  static getColor(color) {
    // Helpful map of Activity and/or State colors.
    const colorMap = {
      'Aquamarine': [193, 226, 236],
      'Cadet Blue': [92, 144, 198],
      'Dodger Blue': [66, 130, 198],
      'Hot Pink': [245, 105, 171],
      'Khaki': [249, 217, 119],
      'Lavender': [218, 154, 190],
      'Orange': [249, 189, 133],
      'Orange Red': [244, 145, 19],
      'Pink': [245, 213, 228],
      'Plum': [176, 150, 193],
      'Purple': [144, 111, 169],
      'Salmon': [255, 191, 193],
      'Sky Blue': [166, 203, 240],
      'Spring Green': [124, 191, 183],
      'Violet Red': [183, 80, 163],
      'Yellow': [245, 202, 46],
      'color1': [0x5f, 0x99, 0x00],
      'color2': [0x00, 0x93, 0xc3],
      'color3': [0xee, 0x99, 0x33],
      'color4': [0xcF, 0x30, 0x30],
      'color5': [0x89, 750, 0xD9],
      'color6': [0x3D, 0x3A, 0xAD],
      'color7': [0xEC, 0x82, 0xB2],
      'color8': [0x57, 0x57, 0x57],
    };

    if (colorMap[color]) {
      return colorMap[color];
    }
    return [0, 0, 0];
  }

  /**
   * Get day of year on a Date.
   * Taken from ctl.js to lessen dependency on the library.
   *
   * @static
   * @param {Date} date
   * @returns
   *
   * @memberof FalconUtils
   */
  static getDOY(date) {
    const d = Date.UTC(date.getUTCFullYear(), 0, 0);
    return Math.floor((date.getTime() - d) / 8.64e+7);
  }

  /**
   * Finds a max time range for a list of points.
   * That is the min start time and max end time for all the points.
   *
   * @static
   * @param {any} points
   * @returns
   *
   * @memberof FalconUtils
   */
  static getMaxTimeRange(points) {
    let maxTime = Number.MIN_SAFE_INTEGER;
    let minTime = Number.MAX_SAFE_INTEGER;

    points.forEach((point) => {
      const end = point.duration ? point.start + point.duration : point.start;
      const start = point.start;

      if (start < minTime) {
        minTime = start;
      }

      if (end > maxTime) {
        maxTime = end;
      }
    });

    return { end: maxTime, start: minTime };
  }

  /**
   * Takes an array of `resource` timelineData and converts it to points
   * for drawing in Falcon.
   *
   * @static
   * @param {Array} timelineData
   * @returns
   *
   * @memberof FalconUtils
   */
  static resourceToPoints(timelineData) {
    const points = [];

    timelineData.forEach((data) => {
      const id = data.__document_id;
      const start = this.utc(data['Data Timestamp']);
      const uniqueId = _.uniqueId();
      const value = data['Data Value'];

      points.push({
        id,
        start,
        uniqueId,
        value,
      });
    });

    return points;
  }

  /**
   * Takes an array of `string_xdr` timelineData and converts it to points
   * for drawing in Falcon.
   *
   * @static
   * @param {Array} timelineData
   * @returns
   *
   * @memberof FalconUtils
   */
  static stringXdrToPoints(timelineData) {
    const points = [];

    timelineData.forEach((data, i) => {
      const id = data.__document_id;
      const start = this.utc(data['Data Timestamp']);
      const startTimestamp = data['Data Timestamp'];
      const uniqueId = _.uniqueId();
      const value = data['Data Value'];

      // This may or may not be correct. We're making an assumption that if there's no end,
      // we're going to draw to the end of the day.
      const startTimePlusDelta = this.utc(startTimestamp) + 30;
      const endTimestamp = timelineData[i + 1] !== undefined ? timelineData[i + 1]['Data Timestamp'] : this.timestamp(startTimePlusDelta);
      const duration = this.utc(endTimestamp) - this.utc(startTimestamp);
      const end = start + duration;

      points.push({
        duration,
        end,
        id,
        interpolateEnding: true,
        start,
        uniqueId,
        value,
      });
    });

    return points;
  }

  /**
   * Get a timestamp string from a time.
   *
   * @static
   * @param {Number} time
   * @returns {String}
   *
   * @memberof FalconUtils
   */
  static timestamp(time) {
    const date = new Date(time * 1000);
    const year = date.getUTCFullYear();
    const doy = this.getDOY(date);
    const hour = date.getUTCHours();
    const mins = date.getUTCMinutes();

    let timeStr = '';
    timeStr = `${year}-${this.zeroPad(doy, 3)}T`;
    timeStr += `${this.zeroPad(hour, 2)}:${this.zeroPad(mins, 2)}`;

    const secs = date.getUTCSeconds();
    timeStr += `:${this.zeroPad(secs, 2)}`;

    const msecs = date.getUTCMilliseconds();
    timeStr += `.${this.zeroPad(msecs, 3)}`;

    return timeStr;
  }

  /**
   * Formats a start and end time into a time range string.
   *
   * @static
   * @param {any} start
   * @param {any} end
   *
   * @memberof FalconUtils
   */
  static timestring(start, end) {
    const startStr = this.timestamp(start);
    const endStr = this.timestamp(end);
    const durStr = this.dhms(end - start);

    return `${startStr} - ${endStr} (${durStr})`;
  }

  /**
   * Takes a ISO 8601 time string and returns
   * an epoch number representation of that time.
   *
   * @static
   * @param {String} timeStr
   * @returns
   *
   * @memberof FalconUtils
   */
  static utc(timeStr) {
    const re = /(\d\d\d\d)-(\d\d\d)T(\d\d):(\d\d):(\d\d)\.?(\d\d\d)?/;
    const results = timeStr.toString().match(re);
    let withMilliSeconds = true;

    if (!results) {
      return 0;
    }

    if (!results[6]) {
      withMilliSeconds = false;
    }

    const year = parseInt(results[1], 10);
    const doy = parseInt(results[2], 10);
    const hour = parseInt(results[3], 10);
    const min = parseInt(results[4], 10);
    const sec = parseInt(results[5], 10);
    const msec = withMilliSeconds ? parseInt(results[6], 10) : 0;
    const msecsUTC = Date.UTC(year, 0, doy, hour, min, sec, msec);

    return msecsUTC / 1000;
  }

  /**
   * Helper that zero pads a number and converts it to a string.
   * Taken from ctl.js to lessen dependency on the library.
   *
   * @static
   * @param {Number} num
   * @param {Number} count
   * @returns
   *
   * @memberof FalconUtils
   */
  static zeroPad(num, count) {
    let str = `${num}`;

    while (str.length < count) {
      str = `0${str}`;
    }

    return str;
  }
}

window.FalconUtils = FalconUtils;
