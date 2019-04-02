/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Pipe, PipeTransform } from '@angular/core';
import { dateToTimestring, toDuration } from '../../../shared/util';
import { RavenResourcePoint } from '../../models';

@Pipe({
  name: 'resourcePointValue',
})
export class RavenResourcePointValuePipe implements PipeTransform {
  transform(point: RavenResourcePoint, args?: any): any {
    if (point.isDuration) {
      return toDuration(point.value, true);
    } else if (point.isTime) {
      return dateToTimestring(new Date(point.value * 1000), true);
    }

    return point.value;
  }
}
