(function (window) {
  /**
   * A node in the tree used by falcon source explorer.
   *
   * @class FalconTreeNode
   */
  class FalconTreeNode {
    /**
     * Creates an instance of FalconTreeNode.
     *
     * @memberof FalconTreeNode
     */
    constructor(icon, name, parentId, pinnable) {
      this.actions = [];
      this.draggable = false;
      this.expandable = true;
      this.expanded = false;
      this.icon = icon;
      this.menu = true;
      this.name = name;
      this.openable = false;
      this.opened = false;
      this.parentId = parentId;
      this.pinnable = pinnable;
      this.pinned = false;
      this.selectable = true;
      this.selected = false;
    }
  }

  window.FalconTreeNode = FalconTreeNode;
}(typeof window !== 'undefined' && window));
