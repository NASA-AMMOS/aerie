/* global ActivityBand, ActivityPainter, DrawableInterval, FalconBand */

/**
 * Falcon Activity Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconActivityBand extends FalconBand(Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconActivityBand
   */
  static get is() {
    return 'falcon-activity-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconActivityBand
   */
  static get properties() {
    return {
      /**
       * The height of each activity.
       */
      activityHeight: {
        type: Number,
        value: 20,
      },

      /**
       * The activity bar style.
       *
       * Bar: 1, Line: 2.
       * Using a number other than 1 or 2 has no effect.
       */
      activityStyle: {
        type: Number,
        value: ActivityPainter.BAR_STYLE, // 1.
      },

      /**
       * The layout of activities.
       * This is required! Activities will not draw without this.
       *
       * Compact: 1, Waterfall: 2.
       * Using a number other than 1 or 2 hides all the activities.
       */
      layout: {
        type: Number,
        value: ActivityPainter.COMPACT_LAYOUT, // 1.
      },

      /**
       * List of points we are drawing for this band.
       */
      points: {
        type: Array,
        value: () => [],
      },

      /**
       * Show or hide activity labels.
       */
      showLabel: {
        type: Boolean,
        value: true,
      },

      /**
       * Show label on inside or outside of activity.
       * If true show on inside of activity only. If false show on outside always.
       */
      trimLabel: {
        type: Boolean,
        value: true,
      },
    };
  }

  /**
   * Get the observers of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconActivityBand
   */
  static get observers() {
    return [
      '_activityHeightChanged(activityHeight)',
      '_activityStyleChanged(activityStyle)',
      '_layoutChanged(layout)',
      '_pointsChanged(points)',
      '_showLabelChanged(showLabel)',
      '_trimLabelChanged(trimLabel)',
    ];
  }

  /**
   * Creates an instance of FalconActivityBand.
   *
   * @memberof FalconActivityBand
   */
  constructor() {
    super();

    // Member Vars.
    this.activityBand = new ActivityBand({
      activityHeight: this.activityHeight,
      alignLabel: this.alignLabel,
      autoFit: false,
      baselineLabel: this.baselineLabel,
      borderWidth: this.borderWidth,
      height: this.height,
      heightPadding: this.heightPadding,
      intervals: [],
      label: this.label,
      labelColor: this.labelColor,
      layout: this.layout,
      minorLabels: this.minorLabels,
      name: this.name,
      onDblLeftClick: this._onDblLeftClick.bind(this),
      onHideTooltip: this._onHideTooltip.bind(this),
      onLeftClick: this._onLeftClick.bind(this),
      onRightClick: this._onRightClick.bind(this),
      onShowTooltip: this._onShowTooltip.bind(this),
      onUpdateView: this._onUpdateView.bind(this),
      showLabel: this.showLabel,
      style: this.activityStyle,
      timeAxis: this._timeAxis,
      trimLabel: this.trimLabel,
      viewTimeAxis: this._viewTimeAxis,
    });
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconActivityBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * Observer. Called when activityHeight property changes.
   *
   * @memberof FalconActivityBand
   */
  _activityHeightChanged() {
    if (this.activityBand) {
      this.activityBand.painter.activityHeight = this.activityHeight;
      this.redraw();
    }
  }

  /**
   * Observer. Called when activityStyle property changes.
   *
   * @memberof FalconActivityBand
   */
  _activityStyleChanged() {
    if (this.activityBand && (this.activityStyle === 1 || this.activityStyle === 2)) {
      this.activityBand.painter.style = this.activityStyle;
      this.redraw();
    }
  }

  /**
   * Observer. Called when the layout property changes.
   *
   * @memberof FalconActivityBand
   */
  _layoutChanged() {
    if (this.activityBand) {
      this.activityBand.painter.layout = this.layout;
      this.redraw();
    }
  }

  /**
   * Observer. Called when points property changes.
   *
   * @memberof FalconActivityBand
   */
  _pointsChanged() {
    const intervals = [];

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      const interval = new DrawableInterval({
        color: point.color,
        end: point.end,
        id: point.id,
        label: point.activityName,
        onGetTooltipText: this._onGetTooltipText.bind(this),
        opacity: 0.5,
        properties: {},
        start: point.start,
      });

      // Set the unique ID separately since it is not a DrawableInterval prop.
      interval.uniqueId = point.uniqueId;

      intervals.push(interval);
    }

    intervals.sort(DrawableInterval.earlyStartEarlyEnd);

    this.activityBand.setIntervals(intervals);
  }

  /**
   * Observer. Called when showLabel property changes.
   *
   * @memberof FalconActivityBand
   */
  _showLabelChanged() {
    if (this.activityBand) {
      this.activityBand.painter.showLabel = this.showLabel;
      this.redraw();
    }
  }

  /**
   * Observer. Called when trimLabel property changes.
   *
   * @memberof FalconActivityBand
   */
  _trimLabelChanged() {
    if (this.activityBand) {
      this.activityBand.painter.trimLabel = this.trimLabel;
      this.redraw();
    }
  }
}

customElements.define(FalconActivityBand.is, FalconActivityBand);
