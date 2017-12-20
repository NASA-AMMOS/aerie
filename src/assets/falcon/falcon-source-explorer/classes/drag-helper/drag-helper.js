(function (window) {
  /**
   * Helper class for drag bookkeeping.
   *
   * @class DragHelper
   */
  class DragHelper {
    /**
     * Creates an instance of DragHelper.
     *
     * @memberof DragHelper
     */
    constructor() {
      this.dragging = false;
      this.src = null;
    }

    /**
     * Clear the drag to default values.
     *
     * @memberof DragHelper
     */
    clear() {
      this.dragging = false;
      this.src = null;
    }

    /**
     * Start dragging. Set dragging to true, and drag src.
     *
     * @param {Boolean} dragging
     * @param {Object} src
     *
     * @memberof DragHelper
     */
    start(src) {
      this.dragging = true;
      this.src = src;
    }
  }

  window.DragHelper = DragHelper;
}(typeof window !== 'undefined' && window));
