/* global FalconPolymerUtils, FalconSettings */

/**
 * Falcon Composite Settings Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 * @appliesMixin FalconSettings
 */
class FalconCompositeSettingsBand extends FalconSettings(Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element)) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconCompositeSettingsBand
   */
  static get is() {
    return 'falcon-composite-settings-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconCompositeSettingsBand
   */
  static get properties() {
    return {
      /**
       * The currently selected tab, which indicates the selected
       * sub-band in the composite band.
       */
      selectedTabIndex: {
        type: String,
        value: '0',
      },
    };
  }

  /**
   * Called after the element is detached from the document.
   * Can be called multiple times during the lifetime of an element.
   *
   * @memberof FalconCompositeSettingsBand
   */
  connectedCallback() {
    super.connectedCallback();

    this._addEventListeners();
  }

  /**
   * Add all event listeners.
   *
   * @memberof FalconCompositeSettingsBand
   */
  _addEventListeners() {
    // Get selected band prop change and notify of sub-band change.
    this.addEventListener('falcon-settings-band-selected-band-prop-changed', (e) => {
      e.detail.subBandIndex = this.selectedTabIndex;
      this._fire('falcon-settings-band-selected-sub-band-prop-changed', e.detail);
    });
  }

  /**
   * Return the selected sub-band.
   *
   * @param {any} subBand
   * @returns
   * @memberof FalconCompositeSettingsBand
   */
  _getSelectedSubBand(subBand) {
    for (let i = 0; i < this.selectedBand.bands.length; i += 1) {
      const band = this.selectedBand.bands[i];

      if (band.id === subBand.id) {
        return band;
      }
    }
  }
}

customElements.define(FalconCompositeSettingsBand.is, FalconCompositeSettingsBand);
