import { NgModule } from '@angular/core';
import {
  Params,
  RouterModule,
  RouterStateSnapshot,
  Routes,
} from '@angular/router';
import { RouterStateSerializer } from '@ngrx/router-store';
import {
  AdaptationsComponent,
  PlanComponent,
  PlansComponent,
} from './containers';

const routes: Routes = [
  { path: 'adaptations', component: AdaptationsComponent },
  { path: 'plans/:id', component: PlanComponent },
  { path: 'plans', component: PlansComponent },
  { path: '**', redirectTo: 'plans' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule],
})
export class AppRoutingModule {}

export interface RouterState {
  params: Params;
  path: string;
  queryParams: Params;
  url: string;
}

export class RouterSerializer implements RouterStateSerializer<RouterState> {
  serialize(routerStateSnapshot: RouterStateSnapshot): RouterState {
    const { url, root } = routerStateSnapshot;

    let route = root;
    const path: string[] = [];
    while (route.firstChild) {
      route = route.firstChild;
      if (route.routeConfig && route.routeConfig.path) {
        path.push(route.routeConfig.path);
      }
    }

    const routerState: RouterState = {
      url,
      params: route.params,
      path: path.join('/'),
      queryParams: root.queryParams,
    };

    return routerState;
  }
}
