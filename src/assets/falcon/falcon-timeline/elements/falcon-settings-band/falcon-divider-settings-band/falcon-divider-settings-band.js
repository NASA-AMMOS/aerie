/* global FalconPolymerUtils, FalconSettings */

/**
 * Falcon Divider Settings Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 * @appliesMixin FalconSettings
 */
class FalconDividerSettingsBand extends FalconSettings(Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element)) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconDividerSettingsBand
   */
  static get is() {
    return 'falcon-divider-settings-band';
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconDividerSettingsBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }
}

customElements.define(FalconDividerSettingsBand.is, FalconDividerSettingsBand);
