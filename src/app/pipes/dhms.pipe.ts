import { Pipe, PipeTransform } from '@angular/core';

import { dhms } from '../shared/util';
@Pipe({
  name: 'dhms',
})
export class DhmsPipe implements PipeTransform {
  transform(value: number): string {
    return dhms(value);
  }
}
