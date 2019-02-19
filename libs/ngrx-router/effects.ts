import { Location } from '@angular/common';
import { Injectable } from '@angular/core';
import { ActivationStart, NavigationEnd, Router } from '@angular/router';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { debounce, filter, map, tap } from 'rxjs/operators';
import {
  RouterActionTypes,
  RouterBack,
  RouterForward,
  RouterGo,
  RouterNavigation,
} from './actions';

@Injectable()
export class RouterEffects {
  constructor(
    private actions$: Actions,
    private router: Router,
    private location: Location,
    private store: Store<any>,
  ) {
    this.listenToRouter();
  }

  @Effect({ dispatch: false })
  navigate$ = this.actions$.pipe(
    ofType<RouterGo>(RouterActionTypes.RouterGo),
    map((action: any) => action.payload),
    tap(({ path, queryParams, extras }) =>
      setTimeout(() => this.router.navigate(path, { queryParams, ...extras })),
    ),
  );

  @Effect({ dispatch: false })
  navigateBack$ = this.actions$.pipe(
    ofType<RouterBack>(RouterActionTypes.RouterBack),
    tap(() => setTimeout(() => this.location.back())),
  );

  @Effect({ dispatch: false })
  navigateForward$ = this.actions$.pipe(
    ofType<RouterForward>(RouterActionTypes.RouterForward),
    tap(() => setTimeout(() => this.location.forward())),
  );

  private navEnd$ = this.router.events.pipe(
    filter(event => event instanceof NavigationEnd),
  );

  private listenToRouter() {
    this.router.events
      .pipe(
        filter(event => event instanceof ActivationStart),
        debounce(() => this.navEnd$),
      )
      .subscribe((event: any) => {
        let route = event.snapshot;
        const path: any[] = [];
        const { params, queryParams, data } = route;

        while (route.parent) {
          if (route.routeConfig && route.routeConfig.path) {
            path.push(route.routeConfig.path);
          }
          route = route.parent;
        }

        const routerState = {
          data,
          params,
          path: path.reverse().join('/'),
          queryParams,
        };

        this.store.dispatch(new RouterNavigation(routerState));
      });
  }
}
