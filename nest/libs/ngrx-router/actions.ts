import { NavigationExtras } from '@angular/router';
import { Action } from '@ngrx/store';

export enum RouterActionTypes {
  NavigateByUrl = '[router] navigate_by_url',
  RouterBack = '[router] back',
  RouterForward = '[router] forward',
  RouterGo = '[router] go',
  RouterNavigation = '[router] navigation',
}

export class NavigateByUrl implements Action {
  readonly type = RouterActionTypes.NavigateByUrl;
  constructor(public url: string) {}
}

export class RouterBack implements Action {
  readonly type = RouterActionTypes.RouterBack;
}

export class RouterForward implements Action {
  readonly type = RouterActionTypes.RouterForward;
}

export class RouterGo implements Action {
  readonly type = RouterActionTypes.RouterNavigation;
  constructor(
    public payload: {
      path: any[];
      queryParams?: object;
      extras?: NavigationExtras;
    },
  ) {}
}

export class RouterNavigation implements Action {
  readonly type = RouterActionTypes.RouterNavigation;
  constructor(
    public payload: {
      path: string;
      params?: any;
      queryParams?: any;
      data?: any;
    },
  ) {}
}

export type RouterActions =
  | NavigateByUrl
  | RouterBack
  | RouterForward
  | RouterGo
  | RouterNavigation;
