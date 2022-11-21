<br>
<div align="center">
  <img alt="Aerie" height="85" src="docs/sphinx_scylladb_theme/static/img/logos/aerie-wordmark-with-background.svg">
</div>
<br>

Aerie is a software framework for modeling spacecraft. Its main features include:

- A Java-based mission modeling library
- A discrete-event simulator
- An embedded TypeScript DSL for defining and executing scheduling goals
- An embedded TypeScript DSL for defining and executing constraints
- An embedded TypeScript DSL for defining and executing activity command expansions
- An embedded TypeScript DSL for defining sequences
- A GraphQL API
- A web-based [client application][ui-repo]

## Getting Started

To get started with a first Aerie deployment head over to the [deployment directory][deployment]. Please visit our [documentation website][documentation] for complete instructions on how to use Aerie.

## Directory Structure

```sh
.
├── .github                     # GitHub metadata
├── constraints                 # Java library for constraint checking
├── contrib                     # Java convenience classes for mission models
├── db-tests                    # Database unit tests
├── deployment                  # Deployment artifacts and documentation
├── docker                      # Additional Dockerfiles for Aerie-specific images
├── docs                        # Documentation
├── e2e-tests                   # End-to-end tests
├── examples                    # Example mission models
├── gradle                      # Gradle Wrapper
├── merlin-driver               # Java library for discrete-event simulation
├── merlin-framework            # Java library for mission modeling
├── merlin-framework-junit      # Extension of JUnit to unit test mission models
├── merlin-framework-processor  # Java annotation processor for mission models
├── merlin-sdk                  # Java interface between mission models and the merlin-driver
├── merlin-server               # Service for planning and simulation
├── merlin-worker               # Worker for executing simulations
├── parsing-utilities           # Java classes for JSON serialization and deserialization
├── scheduler-driver            # Java library for goal-oriented scheduling
├── scheduler-server            # Service for scheduling
├── scheduler-worker            # Worker for executing scheduling goals
├── sequencing-server           # Service for sequence generation and management
└── third-party                 # External Java dependencies that are not obtained from Maven
```

## Want to help?

Want to file a bug, contribute some code, or improve documentation? Excellent! Read up on our guidelines for [contributing][contributing]. If you are a developer you can get started quickly by reading the [developer documentation][dev].

## License

The scripts and documentation in this project are released under the [MIT License](LICENSE).

[contributing]: ./CONTRIBUTING.md
[deployment]: ./deployment
[dev]: ./docs/old_site/deployment/building.md
[documentation]: https://nasa-ammos.github.io/aerie
[ui-repo]: https://github.com/NASA-AMMOS/aerie-ui
