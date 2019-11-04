import { Location } from '@angular/common';
import { Injectable } from '@angular/core';
import { ActivationStart, NavigationEnd, Router } from '@angular/router';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { debounce, filter, map, tap } from 'rxjs/operators';
import {
  NavigateByUrl,
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
  navigateByUrl$ = this.actions$.pipe(
    ofType<NavigateByUrl>(RouterActionTypes.NavigateByUrl),
    map((action: NavigateByUrl) => action.url),
    tap(url => this.router.navigateByUrl(url)),
  );

  @Effect({ dispatch: false })
  routerBack$ = this.actions$.pipe(
    ofType<RouterBack>(RouterActionTypes.RouterBack),
    tap(() => setTimeout(() => this.location.back())),
  );

  @Effect({ dispatch: false })
  routerForward$ = this.actions$.pipe(
    ofType<RouterForward>(RouterActionTypes.RouterForward),
    tap(() => setTimeout(() => this.location.forward())),
  );

  @Effect({ dispatch: false })
  routerGo$ = this.actions$.pipe(
    ofType<RouterGo>(RouterActionTypes.RouterGo),
    map((action: any) => action.payload),
    tap(({ path, queryParams, extras }) =>
      setTimeout(() => this.router.navigate(path, { queryParams, ...extras })),
    ),
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
