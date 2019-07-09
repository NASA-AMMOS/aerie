# Scaffolding

Most of the files that are required for a feature can be generated with the
[`@ngrx/scaffold` package][scaffold] and `ng generate`. These are the steps
that are required to generate all files for a new feature. Run the following
commands from the `src/app/falcon` directory.

1. Change into the module directory that you are working on (e.g. falcon, merlin, raven)
2. Change the reducer and feature schematic in angular.json (project root)
   to point to the store in your current directory.
3. Generate actions, effects, and reducers. *Do not prefix*.
```bash
ng generate feature ActivityType
```
4. Generate the component. *Prefix with Raven*. 
```bash
ng generate component RavenActivityTypeList
```
5. Move the component into `shared/components`
```bash
mv raven-activity-type-list ../shared/components/
```
6. Create a component module in the component folder
```bash
touch ../shared/components/raven-activity-type-list/raven-activity-type-list.module.ts
```
7. Add the module definition
```ts
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RavenActivityTypeListComponent } from './raven-activity-type-list.component';

@NgModule({
  declarations: [RavenActivityTypeListComponent],
  exports: [RavenActivityTypeListComponent],
  imports: [ CommonModule],
})
export class RavenActivityTypeListModule {}
```
8. Add the module to `../shared/components/modules.ts`
```diff
+ export * from './raven-activity-type-list/raven-activity-type-list.module';
```
9. Remove the component from `falcon.module.ts` or the module that
   corresponds to whatever directory you are in.
```diff
- import { RavenActivityTypeListComponent } from './raven-activity-type-list/raven-activity-type-list.component';
- declarations: [RavenActivityTypeListComponent],
```

[scaffold]: https://github.com/ngrx/platform/blob/master/docs/schematics/README.md
