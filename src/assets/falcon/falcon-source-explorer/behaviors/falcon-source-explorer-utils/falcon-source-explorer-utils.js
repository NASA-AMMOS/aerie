(function (window) {
  /**
   * Common Falcon Source Explorer utility functions that can be used in any component.
   *
   * @polymerBehavior
   */
  const FalconSourceExplorerUtils = {
    /**
     * Filters nodes in the tree based on a searchTerm.
     *
     * @param {String} searchTerm
     * @returns {Boolean} If true the node is included. False otherwise.
     *
     * @memberof FalconSourceExplorerUtils
     */
    _filter(searchTerm) {
      return (node) => {
        let include = false;

        if (!searchTerm) return true;
        if (!node) return false;

        // If any of a nodes descendants include the search term, then include the node.
        this.bfs((n) => {
          if (n.name.toLowerCase().includes(searchTerm.toLowerCase())) {
            include = true;
          }
        }, node);

        return include;
      };
    },

    /**
     * Helper that dirty refreshes all children nodes by deep cloning the nodes children and re-setting them.
     *
     * @param {String} node
     */
    _refreshChildren(node) {
      if (this[node] && this[node][this.childrenKey]) {
        this.set(`${node}.${this.childrenKey}`, JSON.parse(JSON.stringify(this[node][this.childrenKey])));
      }
    },
  };

  window.FalconSourceExplorerUtils = FalconSourceExplorerUtils;
}(typeof window !== 'undefined' && window));
