(function (window) {
  /**
   * Turns a component into an n-ary tree.
   *
   * @polymer
   * @mixinFunction
   */
  const GenericTree = superClass => class extends superClass {
    /**
     * Get the properties of this mixin.
     *
     * @readonly
     * @static
     *
     * @memberof GenericTree
     */
    static get properties() {
      return {
        /**
         * Key used for the children array of a node.
         */
        childrenKey: {
          type: String,
          value: 'children',
        },

        /**
         * Object defines the hierarchy of displayed data.
         * Contains a list of children nodes.
         */
        data: {
          notify: true,
          type: Object,
          value: () => {
            return null;
          },
        },

        /**
         * Root node in the tree.
         */
        rootNode: {
          type: Object,
          value: () => {
            return null;
          },
        },
      };
    }

    /**
     * Add a newNode to a parentNode.
     *
     * @param {Object} newNode
     * @param {String} parentNode
     *
     * @memberof GenericTree
     */
    add(newNode, parentNode) {
      newNode = Object.assign({}, newNode);
      parentNode = Object.assign({}, parentNode);

      let parent = null;
      let added = false;

      this.dfs((node) => {
        if (node.id === parentNode.id) {
          parent = node;
        }
      });

      if (parent && parent[this.childrenKey]) {
        newNode.parentId = parentNode.id;
        parent[this.childrenKey].push(newNode);
        added = true;
      }

      return added;
    }

    /**
     * Breadth first search a tree.
     *
     * @param {Function} callback
     * @param {Object} startNode
     *
     * @memberof GenericTree
     */
    bfs(callback, startNode) {
      const queue = [];

      queue.unshift(startNode || this.data);
      let currentTree = queue.pop();

      while (currentTree) {
        const length = currentTree[this.childrenKey] ? currentTree[this.childrenKey].length : 0;

        for (let i = 0; i < length; i += 1) {
          if (currentTree[this.childrenKey][i]) {
            queue.unshift(currentTree[this.childrenKey][i]);
          }
        }

        if (callback) {
          callback(currentTree);
        }

        currentTree = queue.pop();
      }
    }

    /**
     * Helper that gives a node's children based on childrenKey.
     *
     * @returns {Array}
     *
     * @memberof GenericTree
     */
    getDataChildren() {
      if (this.data) {
        return this.data[this.childrenKey];
      }
    }

    /**
     * Depth first search a tree.
     *
     * @param {Function} callback
     * @param {Object} startNode
     *
     * @memberof GenericTree
     */
    dfs(callback, startNode) {
      const self = this;

      (function search(currentNode) {
        if (currentNode) {
          const length = currentNode[self.childrenKey] ? currentNode[self.childrenKey].length : 0;

          for (let i = 0; i < length; i += 1) {
            search(currentNode[self.childrenKey][i]);
          }

          if (callback) {
            callback(currentNode);
          }
        }
      }(startNode || self.data));
    }

    /**
     * Returns a flat list of nodes in the tree.
     *
     * @returns {Array} A flat list of all nodes in the tree.
     *
     * @memberof GenericTree
     */
    flatten() {
      const flatTree = [];

      this.dfs((node) => {
        flatTree.push(node);
      });

      return flatTree;
    }

    /**
     * Find an index of an id in an array.
     *
     * @param {Array} arr
     * @param {String} id
     * @returns {Number}
     *
     * @memberof GenericTree
     */
    index(arr, id) {
      let index = -1;

      for (let i = 0; i < arr.length; i += 1) {
        if (arr[i].id === id) {
          index = i;
          break;
        }
      }

      return index;
    }

    /**
     * Move a node in the tree 'from' node -> 'to' node.
     *
     * @param {Object} from
     * @param {Object} to
     *
     * @memberof GenericTree
     */
    move(from, to) {
      if (from && to) {
        const added = this.add(from, to);

        // Since we are doing a move, only remove a node after it was successfully added to the new position.
        if (added) {
          this.remove(from);
        }
      }
    }

    /**
     * Remove an oldNode from the tree.
     *
     * @param {Object} oldNode
     *
     * @memberof GenericTree
     */
    remove(oldNode) {
      oldNode = Object.assign({}, oldNode);

      let parent = null;
      let removed = null;

      this.dfs((node) => {
        if (node.id === oldNode.parentId) {
          parent = node;
        }
      });

      if (parent && parent[this.childrenKey]) {
        const index = this.index(parent[this.childrenKey], oldNode.id);

        if (index < 0) {
          throw new Error('Generic Tree: remove(): Node to remove does not exist.');
        }
        else {
          removed = parent[this.childrenKey].splice(index, 1);
        }
      }
      else {
        throw new Error("Generic Tree: remove(): Cannot remove a node from a non-existing parent. Are you sure all your parentId's are correct?");
      }

      return removed;
    }
  };

  window.GenericTree = GenericTree;
}(typeof window !== 'undefined' && window));
