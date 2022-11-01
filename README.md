<br>
<div align="center">
  <img src="docs/sphinx_scylladb_theme/static/img/logos/aerie-wordmark-light.svg" height="50">
</div>
<br>

Aerie is a software framework for modeling spacecraft. Its main features include:

- A Java-based mission modeling library
- A discrete-event simulator
- An embedded TypeScript DSL for defining and executing scheduling goals
- An embedded TypeScript DSL for defining and executing constraints
- An embedded TypeScript DSL for defining and executing activity command expansions
- A sequence definition editor
- A GraphQL API
- A web-based [client application][ui-repo]

## Getting Started

To get started with a first Aerie deployment head over to the [deployment directory][deployment]. Please visit our [documentation website][documentation] for complete instructions on how to use Aerie.

## Directory Structure

```sh
.
├── .github                     # GitHub metadata
├── command-expansion-server    # Service for sequence generation and management
├── constraints                 # Java library for constraint checking
├── contrib                     # Java convenience classes for mission models
├── db-tests                    # Database unit tests
├── deployment                  # Deployment artifacts and documentation
├── docs                        # Documentation
├── e2e-tests                   # End-to-end tests
├── examples                    # Example mission models
├── gradle                      # Gradle Wrapper
├── merlin-driver               # Simulation engine and driver
├── merlin-framework            # Java library for mission modeling
├── merlin-framework-junit      # Extension of JUnit to unit test mission models
├── merlin-framework-processor  # Java annotation processor for mission models
├── merlin-sdk                  # Java interface between mission models and the merlin-driver
├── merlin-server               # Service for planning and simulation
├── merlin-worker               # Worker for executing simulations
├── parsing-utilities           # Java classes for JSON serialization and deserialization
├── scheduler                   # Java library for goal-oriented scheduling
├── scheduler-server            # Service for scheduling
└── third-party                 # Third party JAR files
```

## Want to help?

Want to file a bug, contribute some code, or improve documentation? Excellent! Read up on our guidelines for [contributing][contributing]. If you are a developer you can get started quickly by reading the [developer documentation][dev].

## License

The scripts and documentation in this project are released under the [MIT License](LICENSE).

[contributing]: ./CONTRIBUTING.md
[deployment]: ./deployment
[dev]: ./docs/source/deployment/building.md
[documentation]: https://nasa-ammos.github.io/aerie
[ui-repo]: https://github.com/NASA-AMMOS/aerie-ui
