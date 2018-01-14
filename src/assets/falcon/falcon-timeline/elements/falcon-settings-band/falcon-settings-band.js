/* global FalconPolymerUtils, FalconSettings */

/**
 * Falcon Settings Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 * @appliesMixin FalconSettings
 */
class FalconSettingsBand extends FalconSettings(Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element)) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconSettingsBand
   */
  static get is() {
    return 'falcon-settings-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconSettingsBand
   */
  static get properties() {
    return {
      /**
       * List of bands displayed in <falcon-timeline>.
       */
      bands: {
        type: Array,
        value: () => [],
      },

      /**
       * The label width of all the bands in the timeline.
       */
      labelWidth: {
        type: Number,
        value: 100,
      },
    };
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconSettingsBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * Event Handler. Called when band height changes in paper-slider.
   *
   * @param {any} e
   *
   * @memberof FalconSettingsBand
   */
  _onBandHeightChanged(e) {
    const newValue = this._getElement(e).value;

    this._onSelectedBandPropChanged('height', parseInt(newValue, 10.0));
  }

  /**
   * Event Handler. Called when label width changes in paper-slider.
   *
   * @param {any} e
   *
   * @memberof FalconSettingsBand
   */
  _onLabelWidthChanged(e) {
    const newValue = this._getElement(e).value;

    // Fire global event since labelWidth needs to change for all bands.
    this._fire('falcon-settings-band-prop-changed', { name: 'labelWidth', value: parseInt(newValue, 10.0) });
  }
}

customElements.define(FalconSettingsBand.is, FalconSettingsBand);
