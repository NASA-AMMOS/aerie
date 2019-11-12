import { ROUTER_NAVIGATED, RouterNavigatedAction } from '@ngrx/router-store';
import { Action } from '@ngrx/store';
import { MonoTypeOperatorFunction, OperatorFunction } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { RouterState } from '../app-routing.module';

export function isRoute(route: string): (action: Action) => boolean {
  return (action: Action) => {
    if (action.type === ROUTER_NAVIGATED) {
      const routerAction = action as RouterNavigatedAction<RouterState>;
      const { path } = routerAction.payload.routerState;
      return route === path;
    }
    return false;
  };
}

export function ofRoute(route: string): MonoTypeOperatorFunction<Action> {
  return filter<Action>(isRoute(route));
}

export function mapToParam<T>(
  key: string,
): OperatorFunction<RouterNavigatedAction<RouterState>, T> {
  return map<RouterNavigatedAction<RouterState>, T>(
    routerAction => routerAction.payload.routerState.params[key],
  );
}
