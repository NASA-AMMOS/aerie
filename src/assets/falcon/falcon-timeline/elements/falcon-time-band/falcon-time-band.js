/* global FalconBand, TimeBand */

/**
 * Falcon Time Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconTimeBand extends FalconBand(Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeBand
   */
  static get is() {
    return 'falcon-time-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeBand
   */
  static get properties() {
    return {
      /**
       * Time band label.
       */
      label: {
        type: String,
        value: 'UTC',
      },

      /**
       * Time it takes between scroll updates.
       */
      scrollDelta: {
        type: Number,
        value: 21600,
      },

      /**
       * The current time range we are viewing in the timeline.
       */
      viewTimeRange: {
        notify: true,
        type: Object,
        value: () => {
          return {
            end: 0,
            start: 0,
          };
        },
      },

      /**
       * Time it takes between zoom updates.
       */
      zoomDelta: {
        type: Number,
        value: 21600,
      },
    };
  }

  /**
   * Creates an instance of FalconTimeBand.
   *
   * @memberof FalconTimeBand
   */
  constructor() {
    super();

    // Member Vars.
    this.timeBand = new TimeBand({
      font: 'normal 9px Verdana',
      height: 37,
      label: this.label,
      minorLabels: this.minorLabels,
      // onFormatNow: () => {},
      // onFormatTimeTick: () => {},
      onHideTooltip: this._onHideTooltip.bind(this),
      onShowTooltip: this._onShowTooltip.bind(this),
      onUpdateView: this._onUpdateView.bind(this),
      scrollDelta: this.scrollDelta,
      timeAxis: this._timeAxis,
      viewTimeAxis: this._viewTimeAxis,
      zoomDelta: this.zoomDelta,
    });
  }

  /**
   * CTL Callback. Called after a Time Band drag.
   *
   * @param {Number} start
   * @param {Number} end
   *
   * @memberof FalconTimeBand
   */
  _onUpdateView(start, end) {
    if (start !== 0 && end !== 0 && start < end) {
      this.set('viewTimeRange', { end, start });
    }
  }
}

customElements.define(FalconTimeBand.is, FalconTimeBand);
