# Standards

## Containers

The `container` folder should contain app containers. Containers are Angular Components with selectors prefixed with `app`, and are responsible for
connecting to the ngrx Store to select state and dispatch events. You can think of a container as being tightly coupled to it's application. It would be hard to move a container to use in another application.

## Components

The `component` folder should contain pure Angular Components with selectors prefixed with `raven`. Pure components should be "dumb", meaning they should not have any internal state or any knowledge of the ngrx Store. They should just have `Inputs` and `Outputs`. 
All a pure components state is passed in the `Inputs`. 
Anytime a component needs to communicate to it's parent it should emit an `Output`.
Pure components should use the `OnPush` [change detection strategy](https://angular.io/api/core/ChangeDetectionStrategy).
It should be easy to move a pure component into another application with little to no work at all.
