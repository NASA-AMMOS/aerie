/* global FalconPolymerUtils, FalconSettings */

/**
 * Falcon Resource Settings Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 * @appliesMixin FalconSettings
 */
class FalconResourceSettingsBand extends FalconSettings(Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element)) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconResourceSettingsBand
   */
  static get is() {
    return 'falcon-resource-settings-band';
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconResourceSettingsBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * DOM Event. Called when fill on/off is selected.
   *
   * @param {any} event
   * @memberof FalconResourceSettingsBand
   */
  _onSelectFill(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._onSelectedBandPropChanged('fill', newValue === 'true');
    }
  }

  /**
   * DOM Event. Called when an type interpolation is selected.
   *
   * @param {any} event
   * @memberof FalconResourceSettingsBand
   */
  _onSelectInterpolation(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._onSelectedBandPropChanged('interpolation', newValue);
    }
  }

  /**
   * DOM Event. Called when rescale is selected.
   *
   * @param {any} event
   * @memberof FalconResourceSettingsBand
   */
  _onSelectRescale(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._onSelectedBandPropChanged('rescale', newValue === 'true');
    }
  }

  /**
   * DOM Event. Called when a show icon is selected.
   *
   * @param {any} event
   * @memberof FalconResourceSettingsBand
   */
  _onSelectShowIcon(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._onSelectedBandPropChanged('showIcon', newValue === 'true');
    }
  }
}

customElements.define(FalconResourceSettingsBand.is, FalconResourceSettingsBand);
