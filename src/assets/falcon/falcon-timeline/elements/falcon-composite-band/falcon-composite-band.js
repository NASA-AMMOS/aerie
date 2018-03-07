/* global CompositeBand, FalconBand */

/**
 * Falcon Composite Band.
 *
 * @polymer
 * @customElement
 * @appliesMixin FalconBand
 */
class FalconCompositeBand extends FalconBand(Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconCompositeBand
   */
  static get is() {
    return 'falcon-composite-band';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconCompositeBand
   */
  static get properties() {
    return {
      /**
       * Observer for child nodes that are added to this component.
       */
      _nodeObserver: {
        readOnly: true,
        type: Object,
        value: null,
      },
    };
  }

  /**
   * Creates an instance of FalconCompositeBand.
   *
   * @memberof FalconCompositeBand
   */
  constructor() {
    super();

    // Member Vars.
    this.compositeBand = new CompositeBand({
      height: this.height,
      heightPadding: this.heightPadding,
      onDblLeftClick: this._onDblLeftClick.bind(this),
      onHideTooltip: this._onHideTooltip.bind(this),
      onLeftClick: this._onLeftClick.bind(this),
      onRightClick: this._onRightClick.bind(this),
      onShowTooltip: this._onShowTooltip.bind(this),
      onUpdateView: this._onUpdateView.bind(this),
      timeAxis: this._timeAxis,
      viewTimeAxis: this._viewTimeAxis,
    });
  }

  /**
   * Called after the element is attached to the document.
   * Can be called multiple times during the lifetime of an element.
   *
   * @memberof FalconCompositeBand
   */
  connectedCallback() {
    super.connectedCallback();

    this._addNodeObserver();
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconCompositeBand
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * Helper. Adds node observer for adding and removing child nodes to this component.
   *
   * @memberof FalconCompositeBand
   */
  _addNodeObserver() {
    if (!this._nodeObserver) {
      this._nodeObserver = Polymer.dom(this).observeNodes((nodes) => {
        this._processNewNodes(nodes.addedNodes);
      });
    }
  }

  /**
   * Non-Prop Observer. Called when new child nodes are added to this component.
   *
   * @param {any} addedNodes
   *
   * @memberof FalconCompositeBand
   */
  _processNewNodes(addedNodes) {
    for (let i = 0, l = addedNodes.length; i < l; ++i) {
      const node = addedNodes[i];

      switch (node.localName) {
        case 'falcon-activity-band':
          if (!this._hasBand(node.activityBand)) {
            this.compositeBand.addBand(node.activityBand);
            this.redraw();
            node.addEventListener('falcon-activity-band-redraw', () => this.redraw());
          }
          break;
        case 'falcon-resource-band':
          if (!this._hasBand(node.resourceBand)) {
            this.compositeBand.addBand(node.resourceBand);
            this.redraw();
            node.addEventListener('falcon-resource-band-redraw', () => this.redraw());
          }
          break;
        case 'falcon-state-band':
          if (!this._hasBand(node.stateBand)) {
            this.compositeBand.addBand(node.stateBand);
            this.redraw();
            node.addEventListener('falcon-state-band-redraw', () => this.redraw());
          }
          break;
        default:
          break;
      }
    }
  }

  /**
   * Helper that checks if the composite bands already have a band.
   *
   * @param {any} band
   * @returns
   *
   * @memberof FalconCompositeBand
   */
  _hasBand(band) {
    let found = false;

    if (band) {
      for (let i = 0, l = this.compositeBand.bands.length; i < l; ++i) {
        const b = this.compositeBand.bands[i];

        if (b.id === band.id) {
          found = true;
        }
      }
    }

    return found;
  }
}

customElements.define(FalconCompositeBand.is, FalconCompositeBand);
