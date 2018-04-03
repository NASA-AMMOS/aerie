/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'mapKey',
})
export class MapKeyPipe implements PipeTransform {
  transform(value: any): any {
    const keyMap = {
      activityName: 'Activity Name',
      activityType: 'Activity Type',
      duration: 'Duration',
      endTime: 'End Time',
      startTime: 'Start Time',
      value: 'Value',
    };
    return keyMap[value] || value;
  }
}
