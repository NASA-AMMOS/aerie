/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';

import {
  ActivatedRouteSnapshot,
  Resolve,
  RouterStateSnapshot,
} from '@angular/router';

import { Observable } from 'rxjs/Observable';
import { RavenActivityDetail } from '../../shared/models';
import { PlanMockService } from '../../shared/services/plan-mock.service';

@Injectable()
export class ActivityResolver implements Resolve<RavenActivityDetail> {
  constructor(private planService: PlanMockService) {}

  resolve(
    route: ActivatedRouteSnapshot,
    _: RouterStateSnapshot,
  ): Observable<RavenActivityDetail> {
    return this.planService.getActivityDetail(route.params.id);
  }
}
