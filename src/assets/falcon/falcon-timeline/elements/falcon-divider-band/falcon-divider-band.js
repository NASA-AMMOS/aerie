/* global FalconBand, StateBand */

/**
 * Falcon Resource Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconDividerBand extends FalconBand(Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconDividerBand
   */
  static get is() {
    return 'falcon-divider-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconDividerBand
   */
  static get properties() {
    return {
      /**
       * Color of the divider.
       */
      color: {
        type: Array,
        value: () => [255, 255, 255],
      },
    };
  }

  /**
   * Get the observers of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconDividerBand
   */
  static get observers() {
    return [
      '_colorChanged(color)',
    ];
  }

  /**
   * Creates an instance of FalconDividerBand.
   *
   * @memberof FalconDividerBand
   */
  constructor() {
    super();

    // Member Vars.
    this.dividerBand = new StateBand({
      borderWidth: this.borderWidth,
      height: this.height,
      id: `falcon-divider-band-${this.id}`,
      intervals: [],
      label: this.label,
      labelColor: this.labelColor,
      minorLabels: this.minorLabels,
      name: this.name,
      onDblLeftClick: this._onDblLeftClick.bind(this),
      onLeftClick: this._onLeftClick.bind(this),
      onRightClick: this._onRightClick.bind(this),
      timeAxis: this._timeAxis,
      viewTimeAxis: this._viewTimeAxis,
    });
  }

  /**
   * Observer. Called when color property changes.
   *
   * @memberof FalconDividerBand
   */
  _colorChanged() {
    if (this.dividerBand) {
      const [red, green, blue] = this.color;

      this.dividerBand.canvas.style.backgroundColor = `rgb(${red}, ${green}, ${blue})`;

      this.redraw();
    }
  }
}

customElements.define(FalconDividerBand.is, FalconDividerBand);
