/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';

export enum EventType {
  NavigationEvent = 'NavigationEvent',
  ClickEvent = 'ClickEvent',
}

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  trackEvent(evt: EventType, val: string): void {
    // @ts-ignore
    if (window._paq) {
      // @ts-ignore
      window._paq.push(['trackEvent', 'NEST', evt, val]);
    }
  }
}
