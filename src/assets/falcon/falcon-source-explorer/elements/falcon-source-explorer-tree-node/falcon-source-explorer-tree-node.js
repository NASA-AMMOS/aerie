/* global DragHelper, FalconPolymerUtils, FalconSourceExplorerUtils, GenericTree */

(function (window) {
  const dragHelper = new DragHelper();

  /**
   * `falcon-source-explorer-tree-node`
   *
   * ### Styling
   *
   * The following custom properties and mixins are available for styling:
   *
   * Custom property | Description | Default
   * ----------------|-------------|----------
   * `--falcon-source-explorer-tree-node-collapsed-color`           | Highlight color for collapsed node   | `rgb(0, 0, 0)`
   * `--falcon-source-explorer-tree-node-expanded-color`            | Highlight color for expanded node    | `rgb(0, 0, 0)`
   * `--falcon-source-explorer-tree-node-opened-color`              | Highlight color for opened node      | `rgb(0, 0, 0)`
   * `--falcon-source-explorer-tree-node-closed-color`              | Highlight color for closed node      | `rgb(0, 0, 0)`
   * `--falcon-source-explorer-tree-node-disabled-color`            | Highlight color for disabled node    | `rgb(0, 0, 0)`
   * `--falcon-source-explorer-tree-node-selected-background-color` | Background color for selected node   | `rgba(200, 200, 200, 0.5)`

   * @customElement
   * @polymer
   */
  class FalconSourceExplorerTreeNode extends GenericTree(Polymer.mixinBehaviors([FalconPolymerUtils, FalconSourceExplorerUtils], Polymer.Element)) {
    /**
     * Get the name of this element.
     *
     * @readonly
     * @static
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    static get is() {
      return 'falcon-source-explorer-tree-node';
    }

    /**
     * Get the properties of this element.
     *
     * @readonly
     * @static
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    static get properties() {
      return {
        /**
         * If true, displays the dropdown menu.
         */
        _dropdownExpanded: {
          type: Boolean,
          value: false,
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
         * Search term used for filtering nodes in the tree.
         */
        searchTerm: {
          type: String,
          value: '',
        },
      };
    }

    /**
     * Called after the element is attached to the document.
     * Can be called multiple times during the lifetime of an element.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    connectedCallback() {
      super.connectedCallback();
      this._addEventListeners();
    }

    /**
     * Called after the element is detached from the document.
     * Can be called multiple times during the lifetime of an element.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    disconnectedCallback() {
      super.disconnectedCallback();
      this._removeEventListeners();
    }

    /**
     * Add all event listeners.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _addEventListeners() {
      this.addEventListener('dragleave', this._onDragLeave, false);
      this.addEventListener('dragover', this._onDragOver, false);
      this.addEventListener('dragstart', this._onDragStart, false);
      this.addEventListener('drop', this._onDrop, false);

      document.addEventListener('click', this._onDocumentClick.bind(this), true);
    }

    /**
     * Remove all event listeners.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _removeEventListeners() {
      this.removeEventListener('dragleave', this._onDragLeave);
      this.removeEventListener('dragover', this._onDragOver);
      this.removeEventListener('dragstart', this._onDragStart);
      this.removeEventListener('drop', this._onDrop);

      document.removeEventListener('click', null);
    }

    /**
     * Accessor. Returns the parent tree node. Returns `null` if root.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    getParent() {
      return this.domHost.tagName === 'FALCON-SOURCE-EXPLORER-TREE-NODE' ? this.domHost : null;
    }

    /**
     * Accessor. Returns the children tree nodes.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    getChildren() {
      return Polymer.dom(this.root).querySelectorAll('falcon-source-explorer-tree-node');
    }

    /**
     * Event. Called on a dragleave event.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _onDragLeave() {
      this._removeClass('drag-over');
    }

    /**
     * Event. Called on a dragover event.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _onDragOver(e) {
      e.preventDefault(); // Necessary. Allows us to drop.
      e.stopPropagation(); // Make sure parent nodes are not also dragged over.

      this._removeClassAllAncestors('drag-over');

      if (this._dragIsValid()) {
        this._addClass('drag-over');
      }

      return false;
    }

    /**
     * Event. Called on a dragstart event.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _onDragStart(e) {
      if (!dragHelper.dragging) {
        dragHelper.start(this);

        // Firefox requires some basic dataTransfer data to be initially set.
        e.dataTransfer.setData('text/html', this.innerHTML);

        // Set the dragImage to be just the node (without it's children).
        const dragImage = Polymer.dom(this.root).querySelector('falcon-source-explorer-tree-node > div > div > div > span:nth-child(1)');
        e.dataTransfer.setDragImage(dragImage, 0, 0);
      }
    }

    /**
     * Event. Called on a drop event.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _onDrop(e) {
      e.preventDefault();
      e.stopPropagation(); // Stops some browsers from redirecting.

      this._removeClassAll('drag-over');

      if (this._dragIsValid()) {
        this._fire('drop-falcon-source-explorer-tree-node', { from: dragHelper.src.data, to: this.data });
      }

      dragHelper.clear();
    }

    /**
     * DOM Event. Called when an action is clicked.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _action(e) {
      this._fire(e.model.__data.action.event, this);
    }

    /**
     * Event. Called to collapse a node.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _collapse() {
      this.set('data.expanded', false);
      this._fire('collapse-falcon-source-explorer-tree-node', this);
    }

    /**
     * Event. Called to expand a node.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _expand() {
      this.set('data.expanded', true);
      this._fire('expand-falcon-source-explorer-tree-node', this);
    }

    /**
     * Event. Called to close a node.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _close() {
      this.set('data.opened', false);
      this._fire('close-falcon-source-explorer-tree-node', this);
    }

    /**
     * Event. Called to open a node.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _open() {
      this.set('data.opened', true);
      this._fire('open-falcon-source-explorer-tree-node', this);
    }

    /**
     * Event. Called when pin is clicked.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _pin() {
      this.set('data.pinned', true);
      this._fire('pin-falcon-source-explorer-tree-node', this);
    }

    /**
     * Event. Highlights node as the selected node.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _select(event) {
      if (this.data.selectable && !Polymer.dom(event).rootTarget.closest('.falcon-source-explorer-tree-node-actions-icon')) {
        this._fire('select-falcon-source-explorer-tree-node', { node: this, selected: !this.data.selected });
      }
    }

    /**
     * Event. Called when unpin is clicked.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _unpin() {
      this.set('data.pinned', false);
      this._fire('unpin-falcon-source-explorer-tree-node', this);
    }

    /**
     * Event. Called when the menu icon is clicked.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _openDropdown() {
      this.set('_dropdownExpanded', true);
    }

    /**
     * DOM Event. Called for every click in the document.
     * If the dropdown is opened, it closes it.
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _onDocumentClick(event) {
      if (!Polymer.dom(event).rootTarget.closest('.falcon-source-explorer-tree-node-actions-dropdown-item')) {
        this.set('_dropdownExpanded', false);
      }
    }

    /**
     * Helper. Returns the class that highlights a node name and icon based on it's state.
     *
     * @param {object} change An object containing the property that changed and its value.
     * @return {String}
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _computeColor(change) {
      if (change && change.base) {
        const {
          expandable,
          expanded,
          opened,
          openable,
        } = change.base;

        let computedClass = 'falcon-source-explorer-tree-node';

        if (expandable && expanded) {
          computedClass += '-expanded';
        }
        else if (expandable && !expanded) {
          computedClass += '-collapsed';
        }
        else if (openable && opened) {
          computedClass += '-opened';
        }
        else if (openable && !opened) {
          computedClass += '-closed';
        }
        else if (!openable) {
          computedClass += '-disabled';
        }

        return computedClass;
      }
    }

    /**
     * Helper. Returns the class that highlights a node row if it is selected.
     *
     * @param {object} change An object containing the property that changed and its value.
     * @return {String}
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _computeSelected(change) {
      return (change && change.base && change.base.selected) ? 'falcon-source-explorer-tree-node-selected' : '';
    }

    /**
     * Helper. Returns true if menu should be shown. False otherwise.
     *
     * @param {Object} change
     * @returns {Boolean}
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _computeShowMenu(change) {
      if (change && change.base) {
        const { menu, selected } = change.base;

        if (menu && selected) {
          return true;
        }
      }
      return false;
    }

    /**
     * Helper. Returns true if drag is valid. False otherwise.
     *
     * Drag is valid if:
     * 1. A node is not dragged into itself.
     * 2. A child is not dragged into it's direct parent.
     * 3. A node is not dragged into a node that does not have children. (TODO: This may need to change.)
     * 4. A parent is not being dragged into a child or any descendant.
     * 5. TODO. Add more as they arise.
     *
     * Note: 'this' is the drag destination node.
     *
     * @returns
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _dragIsValid() {
      const src = dragHelper.src.data;
      const dest = this.data;

      // 1.
      if (src.id === dest.id) {
        return false;
      }

      // 2.
      if (src.parentId === dest.id) {
        return false;
      }

      // 3.
      if (!dest[this.childrenKey]) {
        return false;
      }

      // 3.
      let valid = true;
      this.dfs((node) => {
        if (dest.id === node.id) {
          valid = false;
        }
      }, src); // Start from src node.
      return valid;
    }

    /**
     * Helper. Add a class to a node.
     *
     * @param {String} name
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _addClass(name) {
      this.classList.add(name);
    }

    /**
     * Helper. Removes a class from the node and all of it's ancestors.
     *
     * @param {String} name
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _removeClassAll(name) {
      this._removeClass(name);
      this._removeClassAllAncestors(name);
    }

    /**
     * Helper. Remove a class from a node.
     *
     * @param {String} name
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _removeClass(name) {
      this.classList.remove(name);
    }

    /**
     * Helper. Removes a class from all a node's ancestors.
     *
     * @param {String} name
     *
     * @memberof FalconSourceExplorerTreeNode
     */
    _removeClassAllAncestors(name) {
      let parent = this.getParent();

      while (parent) {
        parent.classList.remove(name);
        parent = parent.getParent();
      }
    }
  }

  window.customElements.define(FalconSourceExplorerTreeNode.is, FalconSourceExplorerTreeNode);
}(typeof window !== 'undefined' && window));
