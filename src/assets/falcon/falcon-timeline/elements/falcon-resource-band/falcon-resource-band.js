/* global DrawableInterval, FalconBand, ResourceBand, ResourcePainter */

/**
 * Falcon Resource Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconResourceBand extends FalconBand(Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconResourceBand
   */
  static get is() {
    return 'falcon-resource-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconResourceBand
   */
  static get properties() {
    return {
      /**
       * True if we want ticks values calculated by CTL.
       */
      autoTickValues: {
        type: Boolean,
        value: true,
      },

      /**
       * Color of the points or line.
       */
      color: {
        type: Array,
        value: () => [0, 0, 0],
      },

      /**
       * True if we want to fill under a line.
       */
      fill: {
        type: Boolean,
        value: false,
      },

      /**
       * The color of the fill under a line.
       */
      fillColor: {
        type: Array,
        value: () => [0, 0, 0],
      },

      /**
       * Show/hide ticks.
       * True if ticks are shown. False if ticks are hidden.
       */
      hideTicks: {
        type: Boolean,
        value: false,
      },

      /**
       * How to interpolate between points.
       * Possible options are 'none', 'linear', or 'constant'.
       * If any other string is input it defaults to 'none'.
       */
      interpolation: {
        type: String,
        value: 'linear',
      },

      /**
       * List of points we are drawing for this band.
       */
      points: {
        type: Array,
        value: () => [],
      },

      /**
       * Tells if we want the Y-Axis scale to be dynamic or static.
       *
       * If true then the scale is dynamic: it recalculates itself when a zoom occurs.
       * Otherwise the scale is static: it does not recalculate itself. It stays with the scale it was on before the zoom.
       */
      rescale: {
        type: Boolean,
        value: true,
      },

      /**
       * True if we want to explicitly show resource points.
       */
      showIcon: {
        type: Boolean,
        value: false,
      },

      /**
       * Custom tick values.
       * If empty (tickValues.length === 0), and autoTickValues === true then min/max ticks will be calculated automatically by CTL.
       */
      tickValues: {
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
   * @memberof FalconResourceBand
   */
  static get observers() {
    return [
      '_autoTickValuesChanged(autoTickValues)',
      '_colorChanged(color)',
      '_fillChanged(fill)',
      '_fillColorChanged(fillColor)',
      '_hideTicksChanged(hideTicks)',
      '_interpolationChanged(interpolation)',
      '_pointsChanged(points)',
      '_rescaleChanged(rescale)',
      '_showIconChanged(showIcon)',
      '_tickValuesChanged(tickValues)',
    ];
  }

  /**
   * Creates an instance of FalconResourceBand.
   *
   * @memberof FalconResourceBand
   */
  constructor() {
    super();

    // Member Vars.
    this.resourceBand = new ResourceBand({
      autoScale: ResourceBand.VISIBLE_INTERVALS,
      autoTickValues: this.autoTickValues,
      height: this.height,
      heightPadding: this.heightPadding,
      hideTicks: this.hideTicks,
      interpolation: this.interpolation,
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
      painter: new ResourcePainter({
        color: this.color,
        fill: this.fill,
        fillColor: this.fillColor,
        showIcon: this.showIcon,
      }),
      rescale: this.rescale,
      tickValues: this.tickValues,
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
   * @memberof FalconResourceBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * Observer. Called when autoTickValues property changes.
   *
   * @memberof FalconResourceBand
   */
  _autoTickValuesChanged() {
    if (this.resourceBand) {
      this.resourceBand.autoTickValues = this.autoTickValues;

      this.redraw();
    }
  }

  /**
   * Observer. Called when color property changes.
   *
   * @memberof FalconResourceBand
   */
  _colorChanged() {
    if (this.resourceBand) {
      // Setting the painter color does not actually set the color.
      // Adding it here for consistency.
      this.resourceBand.painter.color = this.color;

      // Setting each interval color actually sets the color.
      for (let i = 0, l = this.resourceBand.intervalsList[0].length; i < l; ++i) {
        const interval = this.resourceBand.intervalsList[0][i];
        interval.color = this.color;
      }

      this.redraw();
    }
  }

  /**
   * Observer. Called when fill property changes.
   *
   * @memberof FalconResourceBand
   */
  _fillChanged() {
    if (this.resourceBand) {
      this.resourceBand.painter.fill = this.fill;
      this.redraw();
    }
  }

  /**
   * Observer. Called when fillColor property changes.
   *
   * @memberof FalconResourceBand
   */
  _fillColorChanged() {
    if (this.resourceBand) {
      this.resourceBand.painter.fillColor = this.fillColor;
      this.redraw();
    }
  }

  /**
   * Observer. Called when hideTicks property changes.
   *
   * @memberof FalconResourceBand
   */
  _hideTicksChanged() {
    if (this.resourceBand) {
      this.resourceBand.hideTicks = this.hideTicks;
      this.redraw();
    }
  }

  /**
   * Observer. Called when interpolation property changes.
   *
   * @memberof FalconResourceBand
   */
  _interpolationChanged() {
    if (this.resourceBand) {
      this.resourceBand.interpolation = this.interpolation;
      this.resourceBand.setInterpolation(this.interpolation);
      this.redraw();
    }
  }

  /**
   * Observer. Called when points property changes.
   *
   * @memberof FalconResourceBand
   */
  _pointsChanged() {
    this._intervalById = {};
    const intervals = [];

    for (let i = 0, l = this.points.length; i < l; ++i) {
      const point = this.points[i];

      const interval = new DrawableInterval({
        color: this.color,
        end: point.start,
        endValue: point.value,
        icon: 'circle',
        id: point.id,
        onGetTooltipText: this._onGetTooltipText.bind(this),
        opacity: 0.9,
        properties: {
          Value: point.value,
        },
        start: point.start,
        startValue: point.value,
      });

      // Set the unique ID separately since it is not a DrawableInterval prop.
      interval.uniqueId = point.uniqueId;

      this._intervalById[interval.uniqueId] = interval;
      intervals.push(this._intervalById[interval.uniqueId]);
    }

    intervals.sort(DrawableInterval.earlyStartEarlyEnd);

    this.resourceBand.setIntervals(intervals); // This resets interpolation in CTL so we must re-set it on the next line.
    this.resourceBand.setInterpolation(this.interpolation);
  }

  /**
   * Observer. Called when rescale property changes.
   *
   * @memberof FalconResourceBand
   */
  _rescaleChanged() {
    if (this.resourceBand) {
      this.resourceBand.rescale = this.rescale;
      this.resourceBand.autoScale = this.rescale ? ResourceBand.VISIBLE_INTERVALS : ResourceBand.ALL_INTERVALS;

      this.redraw();
    }
  }

  /**
   * Observer. Called when showIcon property changes.
   *
   * @memberof FalconResourceBand
   */
  _showIconChanged() {
    if (this.resourceBand) {
      this.resourceBand.painter.showIcon = this.showIcon;
      this.redraw();
    }
  }

  /**
   * Observer. Called when tickValues property changes.
   *
   * @memberof FalconResourceBand
   */
  _tickValuesChanged() {
    if (this.resourceBand) {
      this.resourceBand.tickValues = this.tickValues;
      this.redraw();
    }
  }
}

customElements.define(FalconResourceBand.is, FalconResourceBand);
