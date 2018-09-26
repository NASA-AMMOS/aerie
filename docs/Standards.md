# Standards

## Containers

A `container` folder should contain app containers. Containers are Angular Components with selectors prefixed with `app`, and are responsible for
connecting to the ngrx Store to select state and dispatch events. You can think of a container as being tightly coupled to it's application. It would be hard to move a container to use in another application.

## Components

A `component` folder should contain pure Angular Components with selectors prefixed with `raven`. Pure components should be "dumb", meaning they should not have any internal state or any knowledge of the ngrx Store. They should just have `Inputs` and `Outputs`. 
All a pure components state is passed in the `Inputs`. 
Anytime a component needs to communicate to it's parent it should emit an `Output`.
Pure components should use the `OnPush` [change detection strategy](https://angular.io/api/core/ChangeDetectionStrategy).
It should be easy to move a pure component into another application with little to no work at all.

## Alphabetical

All code in this application is alphabetized for clarity. The linter picks up some of these rules, but some rules are up to the developer to maintain. For example arranging action types alphabetically across different files is up to the developer. This should be a focus of code-reviews to maintain.

## Immutability

Most functions in this application should be pure. Meaning they should accept input parameters and return new outputs without any side-effects. This makes the application much easier to test and maintain. More info on pure functions can be found [here](https://en.wikipedia.org/wiki/Pure_function).

## Comments

All functions should have a [JavaDoc](http://typedoc.org/guides/doccomments/) style comment:

```ts
/**
 * Returns the square of the input number.
 */ 
function square(x: number): number {
  return x * x;
}
```

Any other inline comments should use `//`. These comments should start with a capital letter and end with a period:

```ts
// This is a nice comment.
```

## Formatting

Previously, we had to painstakingly format every file with no assistance from
our toolchain. Now, it can be completely automated so that you never have to
format a file again _and_ the entire codebase will use the exact same style.

We use [Prettier][prettier] for formatting. There is a lot of documentation
there, but here is the summary of what Prettier is:

* An opinionated code formatter. It doesn't lint. It just reformats your code into a standard style.
* Supports many languages - JS, TS, CSS, Markdown, YAML, JSX, etc. with tons more in progress.
* Integrates with most editors. VS Code, VIM, Emacs, Atom, Sublime, etc.
* Has few options. It is mostly config free except for the few things that cause holy wars among devs

**Formatting as you go**

In VS Code, install the Prettier extension. Once it is installed you type
`CMD + Shift + P` -> Format Document (also works with a selection) or configure
it to format on save in the per language settings.

[prettier]: https://prettier.io/

**Regarding decorator formatting**

`@Decorators()` are always on their own line. Angular prefers inline decorators
for member variables but Prettier doesn't make that distinction. So that is a
compromise we have to make. Make sure to include a new line between decorated
items because Prettier won't add them for you. Here is an issue which discusses
inline Decorators in Angular: https://github.com/prettier/prettier/issues/4924
and here is the Prettier issue where the decision was made: 
https://github.com/prettier/prettier/issues/2613.
