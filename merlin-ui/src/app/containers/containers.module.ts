import { NgModule } from '@angular/core';

import { AdaptationsModule } from './adaptations';
import { PlanModule } from './plan';
import { PlansModule } from './plans';

const MODULES = [AdaptationsModule, PlanModule, PlansModule];

@NgModule({
  imports: MODULES,
  exports: MODULES,
})
export class ContainersModule {}
