import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';

@Component({
  selector: 'app-toolbar',
  styleUrls: ['./toolbar.component.css'],
  templateUrl: './toolbar.component.html',
})
export class ToolbarComponent implements OnChanges {
  @Input()
  height: number;

  constructor(private ref: ElementRef) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.height) {
      this.ref.nativeElement.style.setProperty('--height', `${this.height}px`);
    }
  }
}
