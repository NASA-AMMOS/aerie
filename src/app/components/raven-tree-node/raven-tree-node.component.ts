/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';

import {
  RavenSource,
  StringTMap,
} from './../../shared/models';

@Component({
  selector: 'raven-tree-node',
  styleUrls: ['./raven-tree-node.component.css'],
  templateUrl: './raven-tree-node.component.html',
})
export class RavenTreeNodeComponent {
  @Input() id: string;
  @Input() tree: StringTMap<RavenSource>;

  @Output() close: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() collapse: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() expand: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() open: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();
  @Output() select: EventEmitter<RavenSource> = new EventEmitter<RavenSource>();

  onClose(e: MouseEvent): void {
    const source = this.getSourceFromMouseEvent(e);
    this.close.emit(source);
  }

  onCollapse(e: MouseEvent): void {
    const source = this.getSourceFromMouseEvent(e);
    this.collapse.emit(source);
  }

  onExpand(e: MouseEvent): void {
    const source = this.getSourceFromMouseEvent(e);
    this.expand.emit(source);
  }

  onOpen(e: MouseEvent): void {
    const source = this.getSourceFromMouseEvent(e);
    this.open.emit(source);
  }

  onSelect(e: MouseEvent): void {
    const source = this.getSourceFromMouseEvent(e);
    this.select.emit(source);
  }

  /**
   * Helper. Gets the source node from original mouse event and properly bubble it up to the parent.
   * This is necessary because raven-tree-node is recursive.
   * See: https://stackoverflow.com/a/42256582
   */
  getSourceFromMouseEvent(e: MouseEvent): RavenSource {
    let source: RavenSource;

    if (e instanceof MouseEvent) {
      e.preventDefault();
      e.stopPropagation();

      // Set the source from a mouse event.
      source = this.tree[this.id];
    } else {
      // If we are not responding to a mouse event just pass along the source from a previous mouse event.
      source = e;
    }

    return source;
  }
}
