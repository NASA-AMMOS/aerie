# Timelines

This library provides tools for querying and manipulating "timelines" from an Aerie plan or set of
simulation results. This includes things like resource profiles, activity instances, and activity directives,
but can be extended to support more kinds if needed.

See [MODULE_DOCS.md](./MODULE_DOCS.md) for a description of the architecture and design of the library.

- Building and testing: `./gradlew :timeline:build`
- Generating a jar for local experimentation: `./gradlew :timeline:shadowJar`
  - jar will be available at `timeline/build/libs/timeline-all.jar`
- Generating documentation: `./gradlew :timeline:dokkaHtml`
  - docs will be available at `timeline/build/dokka/html/index.html`
