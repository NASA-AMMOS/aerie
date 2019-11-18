import { MatMenuTrigger } from '@angular/material';

export class ContextMenu {
  contextMenuPosition = { x: '0px', y: '0px' };

  /**
   * @see https://github.com/angular/components/issues/5007#issuecomment-554124365
   */
  onContextMenu(event: MouseEvent, trigger: MatMenuTrigger, item: any) {
    event.preventDefault();
    this.contextMenuPosition.x = `${event.clientX}px`;
    this.contextMenuPosition.y = `${event.clientY}px`;
    trigger.menuData = { item };
    trigger._openedBy = 'mouse';
    trigger.openMenu();
  }
}
