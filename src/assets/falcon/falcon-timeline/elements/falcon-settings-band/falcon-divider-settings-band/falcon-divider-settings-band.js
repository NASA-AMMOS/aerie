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
}

customElements.define(FalconDividerSettingsBand.is, FalconDividerSettingsBand);
