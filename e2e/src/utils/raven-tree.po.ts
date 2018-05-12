/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  by,
  element,
  ElementFinder,
} from 'protractor';

import {
  clickById,
  probe,
} from './helpers';

export class RavenTree {
  element: ElementFinder;
  id: string;
  legendCount: number;
  name: string;
  tree: any;

  constructor(legendCount: number, name: string) {
    this.legendCount = legendCount;
    this.name = name;
  }

  close() {
    clickById(`raven-tree-${this.name}-close`);
    this.setElement();
  }

  open() {
    clickById(`raven-tree-${this.name}-open`);
    this.setElement();
  }

  async getProp(prop: string) {
    await this.setTree();
    return this.tree[this.id][prop];
  }

  async setElement() {
    this.element = element(by.css(`raven-tree[class=${this.name}]`));
    this.id = await probe(this.element, 'id');
    await this.setTree();
  }

  async setTree() {
    this.tree = await probe(this.element, 'tree');
  }
}
