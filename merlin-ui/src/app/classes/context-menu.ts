import { MatMenuTrigger } from '@angular/material';

export class ContextMenu {
  contextMenuPosition = { x: 0, y: 0 };

  /**
   * @see https://github.com/angular/components/issues/5007#issuecomment-554124365
   */
  onContextMenu(event: MouseEvent, trigger: MatMenuTrigger, item: any) {
    event.preventDefault();
    const { clientX: x, clientY: y } = event;
    this.contextMenuPosition = { x, y };
    trigger.menuData = { item };
    trigger._openedBy = 'mouse';
    trigger.openMenu();
  }
}
