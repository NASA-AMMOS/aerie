/**
 * Copyright 2018, by the California Institute of Technology. ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged.
 * Any commercial use must be negotiated with the Office of Technology Transfer at the California Institute of Technology.
 * This software may be subject to U.S. export control laws and regulations.
 * By accepting this document, the user agrees to comply with all applicable U.S. export laws and regulations.
 * User has the responsibility to obtain export licenses, or other export authority as may be required
 * before exporting such information to foreign countries or providing access to foreign persons
 */

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { config } from '../config';

const merlinModule = config.appModules[0];
const falconModule = config.appModules[1];
const ravenModule = config.appModules[2];

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: ravenModule.path,
  },
  {
    data: { title: merlinModule.title },
    loadChildren: () => import('./planning/planning.module').then(m => m.PlanningModule),
    path: merlinModule.path,
  },
  {
    data: { title: falconModule.title },
    loadChildren: () => import('./sequencing/sequencing.module').then(m => m.SequencingModule),
    path: falconModule.path,
  },
  {
    data: { title: ravenModule.title },
    loadChildren: () => import('./raven/raven.module').then(m => m.RavenModule),
    path: ravenModule.path,
  },
  {
    path: '**',
    redirectTo: ravenModule.path,
  },
];

@NgModule({
  exports: [RouterModule],
  imports: [RouterModule.forRoot(routes, { useHash: true })],
})
export class AppRoutingModule {}
