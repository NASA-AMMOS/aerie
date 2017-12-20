/* global FalconPolymerUtils, FalconSourceExplorerUtils, GenericTree */

(function (window) {
  /**
   * `falcon-source-explorer`
   *
   * @demo demo/index.html
   * @customElement
   * @polymer
   */
  class FalconSourceExplorer extends GenericTree(Polymer.mixinBehaviors([FalconPolymerUtils, FalconSourceExplorerUtils], Polymer.Element)) {
    /**
     * Get the name of this element.
     *
     * @readonly
     * @static
     *
     * @memberof FalconSourceExplorer
     */
    static get is() {
      return 'falcon-source-explorer';
    }

    /**
     * Get the properties of this element.
     *
     * @readonly
     * @static
     *
     * @memberof FalconSourceExplorer
     */
    static get properties() {
      return {
        /**
         * Pointer to the current node.
         * Changes depending on the current tab.
         */
        _currentNode: {
          type: Object,
          value: () => {
            return null;
          },
        },

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

        /**
         * The currently selected tab.
         */
        selectedTabIndex: {
          type: String,
          value: '0',
        },

        /**
         * List of tabs for the tree.
         */
        tabs: {
          type: Array,
          value: () => {
            return [
              { displayName: 'All', id: '0' },
            ];
          },
        },
      };
    }

    /**
     * Get the observers of this element.
     *
     * @readonly
     * @static
     *
     * @memberof FalconSourceExplorer
     */
    static get observers() {
      return [
        '_updateCurrentNode(data, selectedTabIndex)',
      ];
    }

    /**
     * Observer. Updates _currentNode based on the current tab.
     *
     * @memberof FalconSourceExplorer
     */
    _updateCurrentNode() {
      if (this.tabs[this.selectedTabIndex]) {
        this.dfs((node) => {
          if (node.id === this.tabs[this.selectedTabIndex].id) {
            this.set('_currentNode', node);
            this._refreshChildren('_currentNode');
          }
        }, this.rootNode); // Start from the root node.
      }
    }

    /**
     * Event. Called when the `pin-falcon-source-explorer-node` event is fired from an internal node.
     *
     * @param {Event} e
     *
     * @memberof FalconSourceExplorer
     */
    _onPin(e) {
      const { id, name } = e.detail.data;
      this.push('tabs', { displayName: name, id });
    }

    /**
     * Event. Called when the `unpin-falcon-source-explorer-node` event is fired from an internal node.
     *
     * @param {Event} e
     *
     * @memberof FalconSourceExplorer
     */
    _onUnpin(e) {
      const node = e.detail.data;
      const index = this.index(this.tabs, node.id);

      if (index > -1) {
        this.splice('tabs', index, 1);
        this.set('selectedTabIndex', '0'); // Switch back to first tab in-case we are on a tab that has been deleted.
      }
    }
  }

  window.customElements.define(FalconSourceExplorer.is, FalconSourceExplorer);
}(typeof window !== 'undefined' && window));
