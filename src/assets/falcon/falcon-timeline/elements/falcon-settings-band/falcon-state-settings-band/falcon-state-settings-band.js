/* global FalconPolymerUtils, FalconSettings */

/**
 * Falcon Settings Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 * @appliesMixin FalconSettings
 */
class FalconStateSettingsBand extends FalconSettings(Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element)) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconStateSettingsBand
   */
  static get is() {
    return 'falcon-state-settings-band';
  }

  /**
   * DOM Event. Called when an alignLabel is selected.
   *
   * @param {any} event
   * @memberof FalconStateSettingsBand
   */
  _onAlignLabel(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._onSelectedBandPropChanged('alignLabel', parseInt(newValue, 10.0));
    }
  }

  /**
   * DOM Event. Called when an baselineLabel is selected.
   *
   * @param {any} event
   * @memberof FalconStateSettingsBand
   */
  _onBaselineLabel(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._onSelectedBandPropChanged('baselineLabel', parseInt(newValue, 10.0));
    }
  }
}

customElements.define(FalconStateSettingsBand.is, FalconStateSettingsBand);
