/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';

import {
  HttpEvent,
  HttpErrorResponse,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';

import { Observable } from 'rxjs/Observable';

import * as fromRequest from './../reducers/request';
import * as requestAction from './../actions/request';

@Injectable()
export class RequestInterceptor implements HttpInterceptor {
  constructor(private store: Store<fromRequest.RequestState>) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    this.store.dispatch(new requestAction.Pending(true));

    return next
      .handle(request)
      .map((ev: HttpEvent<any>) => {
        if (ev instanceof HttpResponse) {
          this.store.dispatch(new requestAction.Pending(false));
        }

        return ev;
      })
      .catch(response => {
        if (response instanceof HttpErrorResponse) {
          console.error('Processing http error', response);
        }

        return Observable.throw(response);
      });
  }
}
