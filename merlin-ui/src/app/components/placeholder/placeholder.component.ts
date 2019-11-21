import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-placeholder',
  styles: [
    `
      :host {
        background-color: var(--color, #f2f2f2);
        display: block;
        height: var(--height, 200px);
        overflow: hidden;
        width: 100%;
      }
    `,
  ],
  template: '',
})
export class PlaceholderComponent implements OnChanges {
  @Input()
  color: string;

  @Input()
  height: number;

  constructor(private ref: ElementRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.color) {
      this.ref.nativeElement.style.setProperty('--color', `${this.color}`);
    }

    if (changes.height) {
      this.ref.nativeElement.style.setProperty('--height', `${this.height}px`);
    }
  }
}
