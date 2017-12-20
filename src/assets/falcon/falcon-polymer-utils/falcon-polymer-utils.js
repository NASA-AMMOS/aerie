(function (window) {
  /**
   * A Polymer behavior of common utility functions that can be used in any component.
   *
   * @polymerBehavior
   */
  window.FalconPolymerUtils = {
    /**
     * Returns true if a && b.
     * Good for use in template bindings.
     *
     * @param {any} a
     * @param {any} b
     * @returns {Boolean}
     *
     * @memberof FalconPolymerUtils
     */
    _and(a, b) {
      return a && b;
    },

    /**
     * Returns true if 'a' is equal to 'b'.
     * Good for use in template bindings.
     *
     * @param {String} a
     * @param {String} b
     * @returns {Boolean}
     *
     * @memberof FalconPolymerUtils
     */
    _equal(a, b) {
      return a === b;
    },

    /**
     * Fire a generic event with a detail that bubbles and is composed.
     *
     * @param {String} eventName
     * @param {any} detail
     *
     * @memberof FalconPolymerUtils
     */
    _fire(eventName, detail) {
      this.dispatchEvent(new CustomEvent(eventName, {
        bubbles: true,
        composed: true,
        detail,
      }));
    },

    /**
     * Helper to get an element from an event.
     *
     * Some browsers store elements on events differently.
     * This function should find an element on an event regardless of the browser.
     *
     * @param {Event} event
     * @returns
     *
     * @memberof FalconPolymerUtils
     */
    _getElement(event) {
      return event.srcElement || event.target;
    },

    /**
     * Returns true if 'a' is strictly greater than 'b'.
     * Good for use in template bindings.
     *
     * @param {String} a
     * @param {String} b
     * @returns {Boolean}
     *
     * @memberof FalconPolymerUtils
     */
    _gt(a, b) {
      return a > b;
    },

    /**
     * Turns a boolean value to a string.
     *
     * @param {Boolean} b
     * @returns {String} A string value 'true' or 'false'.
     *
     * @memberof FalconPolymerUtils
     */
    _stringifyBool(b) {
      return b ? 'true' : 'false';
    },
  };
}(typeof window !== 'undefined' && window));
