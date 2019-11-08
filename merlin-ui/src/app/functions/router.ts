import { ROUTER_NAVIGATED, RouterNavigatedAction } from '@ngrx/router-store';
import { Action } from '@ngrx/store';
import { MonoTypeOperatorFunction, OperatorFunction } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { RouterState } from '../types';

export function isRoute(route: string): (action: Action) => boolean {
  return (action: Action) => {
    const isRouteAction = action.type === ROUTER_NAVIGATED;
    if (isRouteAction) {
      const routeAction = action as RouterNavigatedAction;
      const uRouterState = routeAction.payload.routerState as unknown;
      const routerState = uRouterState as RouterState;
      const routePath = routerState.path;
      return routePath === route;
    }
    return isRouteAction;
  };
}

export function ofRoute(route: string): MonoTypeOperatorFunction<Action> {
  return filter<Action>(isRoute(route));
}

export function mapToParam<T>(
  key: string,
): OperatorFunction<RouterNavigatedAction, T> {
  return map<RouterNavigatedAction, T>(routerAction => {
    const uRouterState = routerAction.payload.routerState as unknown;
    const routerState = uRouterState as RouterState;
    return routerState.params[key];
  });
}
