import { Action } from '@ngrx/store';
import { MonoTypeOperatorFunction, OperatorFunction } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { RouterActionTypes, RouterNavigation } from './actions';

export function isRoute(route: string | string[] | RegExp) {
  return (action: Action) => {
    const isRouteAction = action.type === RouterActionTypes.RouterNavigation;
    if (isRouteAction) {
      const routeAction = action as RouterNavigation;
      const routePath = routeAction.payload.path;
      if (Array.isArray(route)) {
        return route.indexOf(routePath) > -1;
      } else if (route instanceof RegExp) {
        return route.test(routePath);
      } else {
        return routePath === route;
      }
    }
    return isRouteAction;
  };
}

export function ofRoute(
  route: string | string[] | RegExp,
): MonoTypeOperatorFunction<RouterNavigation> {
  return filter<RouterNavigation>(isRoute(route));
}

export function mapToParam<T>(
  key: string,
): OperatorFunction<RouterNavigation, T> {
  return map<RouterNavigation, T>(action => action.payload.params[key]);
}

export function mapToParams<T>(): OperatorFunction<RouterNavigation, T> {
  return map<RouterNavigation, T>(action => action.payload.params);
}

export function mapToQueryParam<T>(
  key: string,
): OperatorFunction<RouterNavigation, T> {
  return map<RouterNavigation, T>(action => action.payload.queryParams[key]);
}

export function mapToData<T>(
  key: string,
): OperatorFunction<RouterNavigation, T> {
  return map<RouterNavigation, T>(action => action.payload.data[key]);
}
