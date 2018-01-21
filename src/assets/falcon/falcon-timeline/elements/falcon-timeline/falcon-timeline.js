/* global FalconPolymerUtils, Sortable */

/**
 * Falcon Timeline Web Component.
 *
 * @demo demo/index.html
 * @polymer
 * @customElement
 * @appliesMixin FalconPolymerUtils
 */
class FalconTimeline extends Polymer.mixinBehaviors([FalconPolymerUtils], Polymer.Element) {
  /**
   * Get the name of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeline
   */
  static get is() {
    return 'falcon-timeline';
  }

  /**
   * Get the properties of this element.
   *
   * @readonly
   * @static
   *
   * @memberof FalconTimeline
   */
  static get properties() {
    return {
      /**
       * The list of bands displayed in the timeline.
       * To push a newBand to this list use: this.push('bands', newBand).
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

      /**
       * The largest time range calculated from every band in the timeline.
       */
      maxTimeRange: {
        type: Object,
        value: () => {
          return {
            end: 0,
            start: 0,
          };
        },
      },

      /**
       * The current band selected from the list of bands.
       * If a band is selected this object will be one of the elements
       * in the bands list.
       */
      selectedBand: {
        type: Object,
        value: null,
      },

      /**
       * The current time range we are viewing in the timeline.
       */
      viewTimeRange: {
        type: Object,
        value: () => {
          return {
            end: 0,
            start: 0,
          };
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
   * @memberof FalconTimeline
   */
  static get observers() {
    return [
      '_bandsChanged(bands)',
    ];
  }

  /**
   * Called after the element is attached to the document.
   * Can be called multiple times during the lifetime of an element.
   *
   * @memberof FalconTimeline
   */
  connectedCallback() {
    super.connectedCallback();
    this._createSortableContainer();
  }

  /**
   * Called after the element is detached from the document.
   * Can be called multiple times during the lifetime of an element.
   *
   * @memberof FalconTimeline
   */
  disconnectedCallback() {
    super.disconnectedCallback();
    this._destroySortableContainer();
  }

  /**
   * Override the default Polymer _attachDom so we
   * attach to the light DOM instead of the shadow DOM.
   *
   * @param {*} dom
   *
   * @memberof FalconTimeline
   */
  _attachDom(dom) {
    Polymer.dom(this).appendChild(dom);
  }

  /**
   * Observer. Called when bands property changes.
   *
   * @param {Array} bands
   *
   * @memberof FalconTimeline
   */
  _bandsChanged() {
    // Dispatch a full window resize event (which triggers an iron resize) to make sure all bands are sized properly.
    window.dispatchEvent(new Event('resize'));
  }

  /**
   * Helper that creates the main sortable container.
   *
   * @memberof FalconTimeline
   */
  _createSortableContainer() {
    const bandsDiv = Polymer.dom(this).querySelector('.falcon-timeline-bands');

    this._sortableContainer = Sortable.create(bandsDiv, {
      animation: 100,
      delay: 0,
      ghostClass: 'falcon-timeline-sortable-placeholder',
      group: {
        name: 'falcon-timeline-bands',
        put: [
          'falcon-timeline-south-bands',
        ],
      },
      onEnd: event => this.fire('falcon-timeline-sortable-on-end', { event }),
      onUpdate: event => this.fire('falcon-timeline-sortable-on-update', { event }),
      scroll: true,
      scrollSensitivity: 30,
      scrollSpeed: 10,
      sort: true,
    });
  }

  /**
   * Helper that destroys the main sortable container.
   *
   * @memberof FalconTimeline
   */
  _destroySortableContainer() {
    if (this._sortableContainer) {
      this._sortableContainer.destroy();
    }
  }
}

customElements.define(FalconTimeline.is, FalconTimeline);
