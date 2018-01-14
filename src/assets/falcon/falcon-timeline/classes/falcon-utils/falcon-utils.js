/*
  global d3
*/

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
}

window.FalconUtils = FalconUtils;
