/* global FalconBand, FalconPolymerUtils, TimeScrollBar */

/**
 * Falcon Time Scroll Bar.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconTimeScrollBar extends Polymer.mixinBehaviors([FalconPolymerUtils], FalconBand(Polymer.Element)) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeScrollBar
   */
  static get is() {
    return 'falcon-time-scroll-bar';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeScrollBar
   */
  static get properties() {
    return {
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
    };
  }

  /**
   * Creates an instance of FalconTimeScrollBar.
   *
   * @memberof FalconTimeScrollBar
   */
  constructor() {
    super();

    // Member Vars.
    this.timeScrollBar = new TimeScrollBar({
      font: 'normal 9px Verdana',
      height: 15,
      label: '',
      // onFormatTimeTick: () => {},
      onUpdateView: this._onUpdateView.bind(this),
      timeAxis: this._timeAxis,
      updateOnDrag: false,
      viewTimeAxis: this._viewTimeAxis,
    });
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconTimeScrollBar
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * CTL Callback. Called after a Time Scroll Bar drag.
   *
   * @param {Number} start
   * @param {Number} end
   *
   * @memberof FalconTimeScrollBar
   */
  _onUpdateView(start, end) {
    if (start !== 0 && end !== 0 && start < end) {
      this._fire('falcon-update-view-time-range', { end, start });
    }
  }
}

customElements.define(FalconTimeScrollBar.is, FalconTimeScrollBar);
