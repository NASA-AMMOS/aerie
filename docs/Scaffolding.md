# Scaffolding

Most of the files that are required for a feature can be generated with the
[`@ngrx/scaffold` package][scaffold] and `ng generate`. These are the steps
that are required to generate all files for a new feature. Run the following
commands from the `src/app/hummingbird` directory.

1. Generate actions, effects, and reducers. Do not prefix.
```bash
ng generate feature ActivityType
```
2. Generate the component. Prefix with either Raven or Hb. State 
```bash
ng generate component HbActivityTypeList
```
3. Move the component into `shared/components`
```bash
mv hb-activity-type-list ../shared/components/
```
4. Create a component module in the component folder
```bash
touch ../shared/components/hb-activity-type-list/hb-activity-type-list.module.ts
```
5. Add the module definition
```ts
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { HbActivityTypeListComponent } from './hb-activity-type-list.component';

@NgModule({
  declarations: [HbActivityTypeListComponent],
  exports: [HbActivityTypeListComponent],
  imports: [ CommonModule],
})
export class HbActivityTypeListModule {}
```
6. Add the module to `../shared/components/modules.ts`
```diff
+ export * from './hb-activity-type-list/hb-activity-type-list.module';
```
7. Remove the component from `hummingbird.module.ts`
```diff
- import { HbActivityTypeListComponent } from './hb-activity-type-list/hb-activity-type-list.component';
- declarations: [HbActivitityTypeListComponent],
```

[scaffold]: https://github.com/ngrx/platform/blob/master/docs/schematics/README.md
