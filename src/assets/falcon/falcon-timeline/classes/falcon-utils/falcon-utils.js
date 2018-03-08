/**
 * Class with static utility methods for use in Falcon.
 *
 * @polymer
 * @mixinFunction
 */
class FalconUtils {
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
   * Adapted tick mark calculations from d3 https://d3js.org/.
   * Copyright (c) 2010-2016, Michael Bostock.
   * All rights reserved.
   *
   * @static
   * @param {Number} min
   * @param {Number} max
   * @param {Number} height
   * @returns
   * @memberof FalconUtils
   */
  static linear(min, max, height) {
    const start = min;
    const stop = max;
    const count = Math.floor(height / 20);
    const largeNumber = 10000;

    let n = 0;

    const range = (start, stop, step) => {
      // Ticks as exponents if start and stop are extra small or extra large.
      let asExponent = ((start || stop) < -largeNumber || (start || stop) > largeNumber || step < .00001) ? true : false;
      start = Number(start);
      stop = Number(stop);
      start = +start, stop = +stop, step = (n = arguments.length) < 2 ? (stop = start, start = 0, 1) : n < 3 ? 1 : +Number(step);
      n = Math.max(0, Math.ceil((stop - start) / step)) | 0;

      let i = -1;
      let range = new Array(n);

      // Tick precision based on step precision.
      let stepStr = step.toExponential();
      let p = stepStr.split('e')[1];
      let stepPrecision = Math.abs(Number(p));
      let precision = (step % 1 == 0 && !asExponent) ? 0 : stepPrecision;

      while (++i < n) {
        if (asExponent) {
          range[i] = (start + i * step).toExponential(precision);
        }
        else {
          range[i] = (start + i * step).toFixed(precision);
        }
      }
      return range;
    };

    const e10 = Math.sqrt(50);
    const e5 = Math.sqrt(10);
    const e2 = Math.sqrt(2);

    let step0 = Math.abs(stop - start) / Math.max(0, count);
    let step1 = Math.pow(10, Math.floor(Math.log(step0) / Math.LN10));
    let error = step0 / step1;

    if (error >= e10) {
        step1 *= 10;
    }
    else if (error >= e5) {
        step1 *= 5;
    }
    else if (error >= e2) {
        step1 *= 2;
    }

    const step = stop < start ? -step1 : step1;

    const ticks = range(
      Math.floor(start / step) * step,
      Math.ceil(stop / step) * step + step / 2,
      step
    );

    return ticks;
  }
}

window.FalconUtils = FalconUtils;
