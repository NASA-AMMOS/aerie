import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {
  AdaptationsComponent,
  PlanComponent,
  PlansComponent,
} from './containers';

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
