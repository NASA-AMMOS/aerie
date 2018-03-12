import { Pipe, PipeTransform } from '@angular/core';

import { timestamp } from '../shared/util';
@Pipe({
  name: 'DOY',
})
export class DOYPipe implements PipeTransform {
  transform(value: number): string {
    return timestamp(value);
  }
}
