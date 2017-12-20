/* global FalconPolymerUtils, FalconSourceExplorerUtils, GenericTree */

(function (window) {
  /**
   * `falcon-source-explorer-tree`
   *
   * @customElement
   * @polymer
   */
  class FalconSourceExplorerTree extends GenericTree(Polymer.mixinBehaviors([FalconPolymerUtils, FalconSourceExplorerUtils], Polymer.Element)) {
    /**
     * Get the name of this element.
     *
     * @readonly
     * @static
     *
     * @memberof FalconSourceExplorerTree
     */
    static get is() {
      return 'falcon-source-explorer-tree';
    }

    /**
     * Get the properties of this element.
     *
     * @readonly
     * @static
     *
     * @memberof FalconSourceExplorerTree
     */
    static get properties() {
      return {
        /**
         * A list of global actions available in nodes
         * that have a menu.
         */
        globalActions: {
          type: Array,
          value: () => {
            return [];
          },
        },

        /**
         * Allows multiple nodes to be selected.
         * Defaults to false.
         */
        multiSelect: {
          type: Boolean,
          value: false,
        },
      };
    }

    /**
     * Event. Called when the `drop-falcon-source-explorer-tree-tree-node` event is fired from an internal node.
     *
     * @param {Event} e
     *
     * @memberof FalconSourceExplorerTree
     */
    _onDrop(e) {
      this.move(e.detail.from, e.detail.to);
      this._refreshChildren('data');
    }

    /**
     * Event. Called when the `select-falcon-source-explorer-tree-tree-node` event is fired from an internal node.
     *
     * @param {Event} e
     *
     * @memberof FalconSourceExplorerTree
     */
    _onSelect(e) {
      const { node, selected } = e.detail;

      if (!this.multiSelect) {
        // Unselect all nodes.
        this.dfs((n) => {
          n.selected = false;
        }, this.rootNode); // Start from the root node.

        // Either select or deselect the node based on the previous state.
        node.set('data.selected', selected);
        this._refreshChildren('data');
      }
      else {
        node.set('data.selected', selected);
      }
    }
  }

  window.customElements.define(FalconSourceExplorerTree.is, FalconSourceExplorerTree);
}(typeof window !== 'undefined' && window));
