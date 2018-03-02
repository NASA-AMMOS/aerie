/* global FalconPolymerUtils, FalconUtils, Painter, Polymer, ResourceBand, TimeAxis, TimeBand, TimeScrollBar, Tooltip */

/**
 * Falcon Band Mixin.
 *
 * This mixin contains props, observers, and functions
 * that are shared between all bands.
 *
 * @polymer
 * @mixinFunction
 * @appliesMixin Polymer.IronResizableBehavior
 * @appliesMixin FalconPolymerUtils
 */
const FalconBand = superClass => class extends Polymer.mixinBehaviors([Polymer.IronResizableBehavior, FalconPolymerUtils], superClass) {
  /**
   * Get the properties of this mixin.
   *
   * @readonly
   * @static
   *
   * @memberof FalconBand
   */
  static get properties() {
    return {
      /**
       * The Polymer debounce object for de-bouncing iron resize events.
       */
      _onIronResizeDebouncer: {
        readOnly: true,
        type: Object,
        value: () => {
          return new Polymer.Debouncer();
        },
      },

      /**
       * The time axis internal to CTL.
       */
      _timeAxis: {
        readOnly: true,
        type: Object,
        value: () => {
          return new TimeAxis({ end: 0, start: 0 });
        },
      },

      /**
       * The tooltip internal to CTL.
       */
      _tooltip: {
        readOnly: true,
        type: Object,
        value: () => {
          return new Tooltip({});
        },
      },

      /**
       * The view time axis internal to CTL.
       */
      _viewTimeAxis: {
        readOnly: true,
        type: Object,
        value: () => {
          return new TimeAxis({ end: 0, start: 0 });
        },
      },

      /**
       * How to horizontally align the activity or state label.
       *
       * Left: 1, Right: 2, Center: 3.
       * If a value is used other than 1, 2, or 3, then the label is aligned to the left.
       */
      alignLabel: {
        type: Number,
        value: Painter.ALIGN_CENTER, // 3.
      },

      /**
       * How to vertically align the activity or state label.
       *
       * Top: 1, Bottom: 2, Center: 3.
       * If a value is used other than 1, 2, or 3, then the label is aligned to the bottom.
       */
      baselineLabel: {
        type: Number,
        value: Painter.BASELINE_CENTER, // 3.
      },

      /**
       * The width of the border around activities or states.
       */
      borderWidth: {
        type: Number,
        value: 1,
      },

      /**
       * The height of the band.
       */
      height: {
        type: Number,
        value: 100,
      },

      /**
       * Padding above the top of the band.
       */
      heightPadding: {
        type: Number,
        value: 10,
      },

      /**
       * A unique id for this band.
       * Every band should have a unique id incase it needs to be compared to another band.
       */
      id: {
        type: String,
        value: '',
      },

      /**
       * The debounce time for iron resize.
       */
      ironResizeDebounceTime: {
        type: Number,
        value: 200,
      },

      /**
       * Band label.
       */
      label: {
        type: String,
        value: '',
      },

      /**
       * Color of the band label.
       */
      labelColor: {
        type: Array,
        value: () => [0, 0, 0],
      },

      /**
       * Width of the band label.
       */
      labelWidth: {
        type: Number,
        value: 100,
      },

      /**
       * The largest time range calculated from every band in the timeline.
       */
      maxTimeRange: {
        type: Object,
        value: () => {
          return {
            end: 0,
            start: 0,
          };
        },
      },

      /**
       * Labels that appear under the main label.
       */
      minorLabels: {
        type: Array,
        value: () => [],
      },

      /**
       * Band name.
       * If no label is provided, the name is used as a label.
       */
      name: {
        type: String,
        value: '',
      },

      /**
       * Set to true if you don't want to draw the band,
       * but still want it on the DOM.
       */
      noDraw: {
        type: Boolean,
        value: false,
      },

      /**
       * The current point that's selected in the band.
       * The selected point should be highlighted.
       * This object is a reference to a DrawableInterval in CTL.
       */
      selectedPoint: {
        type: Object,
        value: null,
      },

      /**
       * The color of the selected point.
       */
      selectedPointColor: {
        type: Array,
        value: () => [255, 254, 13],
      },

      /**
       * If true tool-tips are shown on hover (default).
       * Tool-tips are not shown otherwise.
       */
      showTooltip: {
        type: Boolean,
        value: true,
      },

      /**
       * The current time range we are viewing in the timeline.
       */
      viewTimeRange: {
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
   * Get the observers of this mixin.
   *
   * @readonly
   * @static
   *
   * @memberof FalconBand
   */
  static get observers() {
    return [
      '_alignLabelChanged(alignLabel)',
      '_baselineLabelChanged(baselineLabel)',
      '_borderWidthChanged(borderWidth)',
      '_heightChanged(height)',
      '_heightPaddingChanged(heightPadding)',
      '_idChanged(id)',
      '_labelChanged(label)',
      '_labelColorChanged(labelColor)',
      '_labelWidthChanged(labelWidth)',
      '_maxTimeRangeChanged(maxTimeRange.start, maxTimeRange.end)',
      '_minorLabelsChanged(minorLabels)',
      '_nameChanged(name)',
      '_viewTimeRangeChanged(viewTimeRange.start, viewTimeRange.end)',
    ];
  }

  /**
   * Called after the element is attached to the document.
   * Can be called multiple times during the lifetime of an element.
   *
   * @memberof FalconBand
   */
  connectedCallback() {
    super.connectedCallback();
    const band = this._getBand();

    this._addEventListeners();

    if (!this.noDraw && band && band.div) {
      band.div.appendChild(this._tooltip.div);
      Polymer.dom(this).appendChild(band.div);
      this._asyncResize();
    }
  }

  /**
   * Called after the element is detached from the document.
   * Can be called multiple times during the lifetime of an element.
   *
   * @memberof FalconBand
   */
  disconnectedCallback() {
    super.disconnectedCallback();
    const band = this._getBand();

    this._removeEventListeners();

    if (!this.noDraw && band && band.div) {
      Polymer.dom(this).removeChild(band.div);
    }
  }

  /**
   * Add all event listeners.
   *
   * @memberof FalconBand
   */
  _addEventListeners() {
    this.addEventListener('click', this._onBandClick);
    this.addEventListener('iron-resize', this._onIronResize);
  }

  /**
   * Remove all event listeners.
   *
   * @memberof FalconBand
   */
  _removeEventListeners() {
    this.removeEventListener('click', this._onBandClick);
    this.removeEventListener('iron-resize', this._onIronResize);
  }

  /**
   * Event listener. Called after component resizes.
   * Debounce the resize since it's redraws.
   * We don't want to redraw too much or CTL might freeze and crash.
   *
   * @memberof FalconBand
   */
  _onIronResize() {
    Polymer.Debouncer.debounce(
      this._onIronResizeDebouncer,
      Polymer.Async.timeOut.after(this.ironResizeDebounceTime),
      () => {
        this.resize();
      },
    );
  }

  /**
   * Helper that gets a band type from 'this'.
   *
   * @readonly
   *
   * @memberof FalconBand
   */
  _getBand() {
    return this.activityBand || this.compositeBand || this.dividerBand || this.resourceBand || this.stateBand || this.timeBand || this.timeScrollBar;
  }

  /**
   * Observer. Called when alignLabel property changes.
   *
   * @memberof FalconBand
   */
  _alignLabelChanged() {
    const band = this._getBand();

    if (band && band.painter) {
      band.painter.alignLabel = this.alignLabel;
      this.redraw();
    }
  }

  /**
   * Observer. Called when baselineLabel property changes.
   *
   * @memberof FalconBand
   */
  _baselineLabelChanged() {
    const band = this._getBand();

    if (band && band.painter) {
      band.painter.baselineLabel = this.baselineLabel;
      this.redraw();
    }
  }

  /**
   * Observer. Called when borderWidth property changes.
   *
   * @memberof FalconBand
   */
  _borderWidthChanged() {
    const band = this._getBand();

    if (band && band.painter) {
      band.painter.borderWidth = this.borderWidth;
      this.redraw();
    }
  }

  /**
   * Observer. Called when height property changes.
   *
   * Note a quirk of CTL is that the height of the TimeBand and TimeScrollBar should be fixed
   * after they are initialized. So we make sure we only update hight for bands that are not
   * TimeBand or TimeScrollBar.
   *
   * @memberof FalconBand
   */
  _heightChanged() {
    const band = this._getBand();

    if (band && !(band instanceof TimeBand) && !(band instanceof TimeScrollBar)) {
      band.height = this.height;

      this._updateCompositeSubBands('height', this.height);
      this._calculateTickValues();

      this.redraw();
    }
  }

  /**
   * Observer. Called when heightPadding property changes.
   *
   * @memberof FalconBand
   */
  _heightPaddingChanged() {
    const band = this._getBand();

    if (band) {
      band.heightPadding = this.heightPadding;
      this._updateCompositeSubBands('heightPadding', this.heightPadding);
      this.redraw();
    }
  }

  /**
   * Observer. Called when id property changes.
   *
   * @memberof FalconBand
   */
  _idChanged() {
    const band = this._getBand();

    if (band) {
      band.div.id = `${this.localName}-${this.id}`;
      band.id = this.id;
      this.redraw();
    }
  }

  /**
   * Observer. Called when label property changes.
   *
   * @memberof FalconBand
   */
  _labelChanged() {
    const band = this._getBand();

    if (band) {
      band.label = this.label;
      this.redraw();
    }
  }

  /**
   * Observer. Called when labelColor property changes.
   *
   * @memberof FalconBand
   */
  _labelColorChanged() {
    const band = this._getBand();

    if (band) {
      band.labelColor = this.labelColor;
      this.redraw();
    }
  }

  /**
   * Observer. Called when labelWidth property changes.
   *
   * @memberof FalconBand
   */
  _labelWidthChanged() {
    this.resize();
  }

  /**
   * Observer. Called when maxTimeRange property changes.
   *
   * @memberof FalconBand
   */
  _maxTimeRangeChanged(start, end) {
    if (start !== 0 && end !== 0) {
      this._timeAxis.updateTimes(start, end);
      this.resize();
    }
  }

  /**
   * Observer. Called when minorLabels property changes.
   *
   * @memberof FalconBand
   */
  _minorLabelsChanged() {
    const band = this._getBand();

    if (band) {
      band.minorLabels = this.minorLabels;
      this.redraw();
    }
  }

  /**
   * Observer. Called when name property changes.
   *
   * @memberof FalconBand
   */
  _nameChanged() {
    const band = this._getBand();

    if (band) {
      band.name = this.name;
      this.redraw();
    }
  }

  /**
   * Observer. Called when viewTimeRange property changes.
   *
   * @param {Number} start
   * @param {Number} end
   *
   * @memberof FalconBand
   */
  _viewTimeRangeChanged(start, end) {
    if (start !== 0 && end !== 0) {
      this._viewTimeAxis.updateTimes(start, end);

      this._calculateTickValues();

      this.resize();
    }
  }

  /**
   * Event. Called when band is clicked.
   * We don't need click events for timeBand or timeScrollBar - those should emit their own events if needed.
   *
   * @param {any} e
   *
   * @memberof FalconBand
   */
  _onBandClick() {
    if (!this.timeBand && !this.timeScrollBar) {
      this._fire('falcon-band-click', { bandId: this.id });
    }
  }

  /**
   * CTL Event. Called when double left click on band.
   *
   * @param {any} e
   * @param {any} ctlData
   *
   * @memberof FalconBand
   */
  _onDblLeftClick(e, ctlData) {
    if (ctlData.interval) {
      this._fire(`${this.localName}-dbl-left-click`, { ctlData });
    }
  }

  /**
   * CTL Event. Called to get interpolated tooltip text.
   *
   * @param {any} e
   * @param {any} ctlData
   *
   * @memberof FalconBand
   */
  _onGetInterpolatedTooltipText() {
    // TODO.
  }

  /**
   * CTL Event. Called to get tooltip text.
   *
   * @param {any} e
   * @param {any} ctlData
   *
   * @memberof FalconBand
   */
  _onGetTooltipText(e, ctlData) {
    if (ctlData.interval) {
      const tooltipText = `
        <table class="tooltiptable">
          <tr>
            <td class="tooltiptablecell">
              Start: ${ctlData.interval.start}
            </td>
            <td class="tooltiptablecell">
              End: ${ctlData.interval.end}
            </td>
          </tr>
        </table>
      `;

      return tooltipText;
    }
  }

  /**
   * CTL Event. Called on hide tooltip.
   *
   * @memberof FalconBand
   */
  _onHideTooltip() {
    this._tooltip.hide();
  }

  /**
   * CTL Event. Called when left click on band.
   *
   * @param {any} e
   * @param {any} ctlData
   *
   * @memberof FalconBand
   */
  _onLeftClick(e, ctlData) {
    if (ctlData.interval) {
      this._selectPoint(ctlData.interval);
      this._fire(`${this.localName}-left-click`, { ctlData });
    }
  }

  /**
   * CTL Event. Called when right click on band.
   *
   * @param {any} e
   * @param {any} ctlData
   *
   * @memberof FalconBand
   */
  _onRightClick(e, ctlData) {
    if (ctlData.interval) {
      this._fire(`${this.localName}-right-click`, { ctlData });
    }
  }

  /**
   * CTL Event. Called on show tooltip.
   *
   * @param {any} e
   * @param {any} text
   *
   * @memberof FalconBand
   */
  _onShowTooltip(e, text) {
    if (this.showTooltip) {
      this._tooltip.show(text, e.clientX, e.clientY);
    }
  }

  /**
   * CTL Event. Called after view is updated.
   *
   * @param {any} e
   * @param {any} ctlData
   *
   * @memberof FalconBand
   */
  _onUpdateView() {
    // TODO.
  }

  /**
   * CTL is really bad at redrawing itself when it's attached to the document.
   * So we add a setTimeout to make sure we resize some time after a band is attached.
   */
  _asyncResize() {
    setTimeout(() => dispatchEvent(new Event('resize')), 0);
  }

  /**
   * Helper that calculates tick values.
   * Only works on a resource band since other bands don't have ticks.
   *
   * @memberof FalconBand
   */
  _calculateTickValues() {
    const band = this._getBand();

    if (band instanceof ResourceBand) {
      band.computeMinMaxPaintValues();
      this.tickValues = FalconUtils.ticks('linear', band.minPaintValue, band.maxPaintValue, this.height);
    }
  }

  /**
   * Resizes the Time Axis and View Time Axis based on the labelWidth and parent offsetWidth.
   * Note: It is very important that the `.timeline-0` class correctly references the parent container.
   *
   * @memberof FalconBand
   */
  _updateTimeAxisXCoordinates() {
    const falcon = document.querySelector('.timeline-0');
    let offsetWidth = 0;

    if (falcon) {
      offsetWidth = falcon.parentElement.offsetWidth;
    }
    else {
      console.error('falcon-timeline: falcon-band: _updateTimeAxisXCoordinates: falcon is null: ', falcon);
    }

    // Update main band.
    this._timeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
    this._viewTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);

    if (this.compositeBand) {
      // Update all sub bands.
      this.compositeBand.bands.forEach((band) => {
        band.timeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
        band.viewTimeAxis.updateXCoordinates(this.labelWidth, offsetWidth);
      });
    }
  }

  /**
   * Helper to select a point in this band.
   *
   * @param {any} interval
   *
   * @memberof FalconBand
   */
  _selectPoint(interval) {
    if (interval) {
      // Don't consider interpolated intervals
      // i.e. intervals that have numbered id's < 0.
      if (typeof interval.id === 'number' && interval.id < 0) {
        return;
      }

      // Reset the selectedPoints original color.
      if (this.selectedPoint) {
        this.selectedPoint.color = this.selectedPoint.originalColor;
      }

      // If the clicked on interval is the same as the selectedPoint,
      // then deselect the point.
      if (this.selectedPoint && this.selectedPoint.id === interval.id) {
        this.selectedPoint = null;
      }
      // Otherwise set the new selectedPoint.
      else {
        this.selectedPoint = interval;
        this.selectedPoint.originalColor = interval.color;
        this.selectedPoint.color = this.selectedPointColor;
      }

      this.redraw();
    }
  }

  /**
   * Helper to update composite bands prop with a value.
   *
   * @param {any} prop
   * @param {any} value
   *
   * @memberof FalconBand
   */
  _updateCompositeSubBands(prop, value) {
    if (this.compositeBand) {
      this.compositeBand.bands.forEach((band) => {
        band[prop] = value;
      });
    }
  }

  /**
   * Redraws the band.
   *
   * @memberof FalconBand
   */
  redraw() {
    const band = this._getBand();

    if (band) {
      this._fire(`${this.localName}-redraw`, null);

      if (!this.noDraw) {
        band.revalidate();
        band.repaint();
      }
    }
  }

  /**
   * Resizes the band.
   *
   * @memberof FalconBand
   */
  resize() {
    this._calculateTickValues();
    this._updateTimeAxisXCoordinates();
    this.redraw();
  }
};

window.FalconBand = FalconBand;
