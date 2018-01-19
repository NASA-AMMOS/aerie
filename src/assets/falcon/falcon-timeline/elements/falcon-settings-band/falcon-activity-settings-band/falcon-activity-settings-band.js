/* global FalconPolymerUtils, FalconSettings */

/**
 * Falcon Activity Settings Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 * @appliesMixin FalconSettings
 */
class FalconActivitySettingsBand extends FalconSettings(Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element)) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconActivitySettingsBand
   */
  static get is() {
    return 'falcon-activity-settings-band';
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconActivitySettingsBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * DOM Event. Called when an activityStyle is selected.
   *
   * @param {any} event
   * @memberof FalconActivitySettingsBand
   */
  _onActivityStyle(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._updateBand('activityStyle', parseInt(newValue, 10.0));
    }
  }

  /**
   * DOM Event. Called when an alignLabel is selected.
   *
   * @param {any} event
   * @memberof FalconActivitySettingsBand
   */
  _onAlignLabel(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._updateBand('alignLabel', parseInt(newValue, 10.0));
    }
  }

  /**
   * DOM Event. Called when an baselineLabel is selected.
   *
   * @param {any} event
   * @memberof FalconActivitySettingsBand
   */
  _onBaselineLabel(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._updateBand('baselineLabel', parseInt(newValue, 10.0));
    }
  }

  /**
   * DOM Event. Called when an layout is selected.
   *
   * @param {any} event
   * @memberof FalconActivitySettingsBand
   */
  _onLayout(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._updateBand('layout', parseInt(newValue, 10.0));
    }
  }

  /**
   * DOM Event. Called when a showLabel is selected.
   *
   * @param {any} event
   * @memberof FalconActivitySettingsBand
   */
  _onShowLabel(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._updateBand('showLabel', newValue === 'true');
    }
  }

  /**
   * DOM Event. Called when a trimLabel is selected.
   *
   * @param {any} event
   * @memberof FalconActivitySettingsBand
   */
  _onTrimLabel(event) {
    const newValue = this._getElement(event).value;

    if (newValue) {
      this._updateBand('trimLabel', newValue === 'true');
    }
  }
}

customElements.define(FalconActivitySettingsBand.is, FalconActivitySettingsBand);
