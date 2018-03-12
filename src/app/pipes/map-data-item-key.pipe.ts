import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'mapKey',
})
export class MapKeyPipe implements PipeTransform {
  transform(value: any): any {
    const keyMap = {
      activityName: 'Activity Name',
      activityType: 'Activity Type',
      startTime: 'Start Time',
      endTime: 'End Time',
      duration: 'Duration',
      value: 'Value',
    };
    return keyMap[value] || value;
  }
}
