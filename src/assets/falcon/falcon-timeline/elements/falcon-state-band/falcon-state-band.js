/* global DrawableInterval, FalconBand, StateBand */

/**
 * Falcon Resource Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconStateBand extends FalconBand(Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconStateBand
   */
  static get is() {
    return 'falcon-state-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconStateBand
   */
  static get properties() {
    return {
      /**
       * List of points we are drawing for this band.
       */
      points: {
        type: Array,
        value: () => [],
      },
    };
  }

  /**
   * Get the observers of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconStateBand
   */
  static get observers() {
    return [
      '_pointsChanged(points)',
    ];
  }

  /**
   * Creates an instance of FalconStateBand.
   *
   * @memberof FalconStateBand
   */
  constructor() {

    super();

    // Member Vars.
    this.stateBand = new StateBand({
      alignLabel: this.alignLabel,
      autoColor: true,
      baselineLabel: this.baselineLabel,
      borderWidth: this.borderWidth,
      height: this.height,
      heightPadding: this.heightPadding,
      intervals: [],
      label: this.label,
      labelColor: this.labelColor,
      minorLabels: this.minorLabels,
      name: this.name,
      onDblLeftClick: this._onDblLeftClick.bind(this),
      onHideTooltip: this._onHideTooltip.bind(this),
      onLeftClick: this._onLeftClick.bind(this),
      onRightClick: this._onRightClick.bind(this),
      onShowTooltip: this._onShowTooltip.bind(this),
      onUpdateView: this._onUpdateView.bind(this),
      timeAxis: this._timeAxis,
      viewTimeAxis: this._viewTimeAxis,
    });
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconStateBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * Observer. Called when points property changes.
   *
   * @memberof FalconStateBand
   */
  _pointsChanged() {
    const intervals = [];

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      const interval = new DrawableInterval({
        end: point.end,
        endValue: point.value,
        id: point.id,
        label: point.value,
        onGetTooltipText: this._onGetTooltipText.bind(this),
        opacity: 0.6,
        properties: {
          Value: point.value,
        },
        start: point.start,
        startValue: point.value,
      });

      // Set the unique ID separately since it is not a DrawableInterval prop.
      interval.uniqueId = point.uniqueId;

      intervals.push(interval);
    }

    intervals.sort(DrawableInterval.earlyStartEarlyEnd);

    this.stateBand.setIntervals(intervals);
  }
}

customElements.define(FalconStateBand.is, FalconStateBand);
