import { NgModule } from '@angular/core';
import { RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { RouterStateSerializer } from '@ngrx/router-store';
import {
  AdaptationsComponent,
  PlanComponent,
  PlansComponent,
} from './containers';
import { RouterState } from './types';

const routes: Routes = [
  { path: 'adaptations', component: AdaptationsComponent },
  { path: 'plans/:id', component: PlanComponent },
  { path: 'plans', component: PlansComponent },
  { path: '**', redirectTo: '/plans' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule],
})
export class AppRoutingModule {}

export class RouterSerializer implements RouterStateSerializer<RouterState> {
  serialize(routerState: RouterStateSnapshot): RouterState {
    let route = routerState.root;
    const path: string[] = ['']; // '' so we get a starting / when we join('/').

    while (route.firstChild) {
      route = route.firstChild;
      if (route.routeConfig && route.routeConfig.path) {
        path.push(route.routeConfig.path);
      }
    }

    const {
      url,
      root: { queryParams },
    } = routerState;
    const { params } = route;

    return { url, params, path: path.join('/'), queryParams };
  }
}
