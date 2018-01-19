/* global tinycolor */

/**
 * Falcon Settings Mixin.
 *
 * This mixin contains props, observers, and functions
 * that are shared between all settings bands.
 *
 * @polymer
 * @mixinFunction
 */
const FalconSettings = superClass => class extends superClass {
  /**
   * Get the properties of this mixin.
   *
   * @readonly
   * @static
   *
   * @memberof FalconSettings
   */
  static get properties() {
    return {
      /**
       * The currently selected band.
       * This will change if a band is selected in <falcon-timeline>.
       * If the band is selected in this component, it will update the selectedBand in <falcon-timeline>.
       */
      selectedBand: {
        type: Object,
        value: () => {
          return {};
        },
      },
    };
  }

  /**
   * Called after the element is detached from the document.
   * Can be called multiple times during the lifetime of an element.
   *
   * @memberof FalconSettings
   */
  connectedCallback() {
    super.connectedCallback();
    this._addEventListeners();
  }

  /**
   * Called after the element is detached from the document.
   *
   * @memberof FalconSettings
   */
  disconnectedCallback() {
    super.disconnectedCallback();
    this._removeEventListeners();
  }

  /**
   * Add all event listeners.
   *
   * @memberof FalconSettings
   */
  _addEventListeners() {
    this.addEventListener('input', this._onInput.bind(this));
  }

  /**
   * Remove all event listeners.
   *
   * @memberof FalconSettings
   */
  _removeEventListeners() {
    this.removeEventListener('input', this._onInput.bind(this));
  }

  /**
   * DOM Event. Called when an input is changed.
   *
   * @param {any} event
   * @memberof FalconSettings
   */
  _onInput(event) {
    const element = this._getElement(event);
    const value = element.value;
    let color = null;

    switch (element.id) {
      case 'falcon-settings-band-label-input':
        this._updateBand('label', value);
        break;
      case 'falcon-divider-settings-band-color-input':
      case 'falcon-resource-settings-band-color-input':
        color = tinycolor(value);
        this._updateBand('color', [color._r, color._g, color._b]);
        break;
      case 'falcon-resource-settings-band-fill-color-input':
        color = tinycolor(value);
        this._updateBand('fillColor', [color._r, color._g, color._b]);
        break;
      default:
        break;
    }
  }

  /**
   * DOM Event. Called when a show tooltip is selected.
   *
   * @param {any} event
   * @memberof FalconSettings
   */
  _onShowTooltip(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._updateBand('showTooltip', newValue === 'true');
    }
  }

  /**
   * Fire event to change all bands.
   *
   * @param {any} prop
   * @param {any} value
   * @memberof FalconSettings
   */
  _updateAllBands(prop, value) {
    this._fire('falcon-settings-update-all-bands', { prop, value });
  }

  /**
   * Fire event when selected band prop changes for any listening parents.
   *
   * @param {any} prop
   * @param {any} value
   * @memberof FalconSettings
   */
  _updateBand(prop, value) {
    this._fire('falcon-settings-update-band', { prop, value });
  }

  /**
   * Helper that returns a hex string from an RGB array.
   *
   * @param {Array} c
   * @returns
   * @memberof FalconSettings
   */
  _getHexColor(c) {
    if (c && c.length) {
      return tinycolor({ b: c[2], g: c[1], r: c[0] }).toHexString();
    }
  }
};

window.FalconSettings = FalconSettings;
