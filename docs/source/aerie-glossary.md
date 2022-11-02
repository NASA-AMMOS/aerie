# Aerie Glossary

## Aerie Domain

**Aerie deployment**: The term 'adaptation' sometimes means an integrated system using some third-party elements. We refer to a particular configuration of the Aerie system as an 'Aerie Deployment', in the context of a broader ground data system (GDS) deployment.

**Banananation**: the tongue-in-cheek named toy mission model used by Aerie to provide extremely simple examples of mission modelling capabilities.

**Planner**: A person responsible for deciding how to concretely achieve a set of mission objectives over the course of a span of time, formalized as a plan, with the expectation that this plan will be executed by other mission elements, including (and especially) by a spacecraft's onboard system. The planner's concern is with achieving those objectives while balancing reward against risk. Planners iteratively perform scheduling and simulation in a feedback loop to refine their plans. Automated schedulers may themselves utilize simulation artifacts to inform their decisions.

**Schedule**:

## Aerie Client User Interface

**Guides-horz/vert**:

**Layers**:

**Rows**:

**Timelines**:

**View**:

## Scheduling

**Rule/Goal**:

## Simulation and Modeling

**Activity**: is a modeled unit of mission system behavior to be utilized for simulations during planning. Activities emit events which can have various effects on modeled resources, and these effects can be modulated through input parameters.

**Argument**: a value given to a function or assigned to an Activity Parameter.

**Call**: is a function in the Merlin simulation modeling API. Call() executes a provided task and blocks until that task has completed. Typically Call() is used by a mission modeler who wants to execute an activity and wait for it's completion before continuing execution of their modeling code. Call() has a few function signatures so that it can be used to call an activity type and arguments or a Java lambda/Runnable.

**Cell**: allows a mission model to express **time-dependent state** in a way that can be tracked and managed by the host system.

**Constraint**: an expression built up with the [Aerie constraints eDSL](./user-guide/ui-api-guide/constraints/index.rst), which evaluates to a set of windows during which the condition(s) defined by the expression is true or false.

**Decomposition**:
A method for modeling the behavior of an activity (root activity) by composing a set of activities. Each composed activity describes some smaller aspect of behavior. The root activity orchestrates the execution of each child activity by interleaving function calls to `spawn()`, `delay()`, and `call()`. The goal of decomposition is to allow mission modelers to modularize their activity modeling code and to provide greater visibility into the simulated behavior of an activity. For example, consider an activity which models taking an observation with a particular spacecraft instrument. The process of taking an observation may include the distinct phases of instrument startup, take observation, and instrument shutdown. The mission modeler can decide to modularize their modeling and use decomposition to model each of the three phases separately and then compose them with a root activity called "observation" which orchestrates the execution of each of the three activities.

**Delay**: is a function in the Merlin simulation modeling API. A modeler can delay (pause) an activity's execution during simulation effectively modeling some passage of simulated time, before resuming further modeling.

**Dynamics**:

**Effect/Event**:

**Mission model**: Aerie has ceased using the term 'adaptation'; Aerie uses the term 'mission model' to denote the modeling code written in Java, packaged as a JAR, and consulted by the Merlin simulator. It is more specific to the purpose of the code, not overloaded with other extant meanings, and better coheres with the modeling domain.

The term 'adaptation' means too many things in too many contexts. Take for example the term 'mission planning adaptation'. It is unclear whether the speaker refers solely to the APGen .aaf files, or also includes any modeling integrations, or includes further the integrated software deployment which produces resource profiles and associated analyses. The term is also very JPL-centric; different terms are used in the wider domains of planning, modeling, and simulation.

**Parameter**: a named variable (and data type) in a function description or Activity definition, utilized within the function/Activity definition.

**Profile**: the **time-dependent evolution** behavior of a resource.

**Register**: is a model of a resource which can have its value set and this value remains until it is set with a different value. The term register here is used to clearly indicate the semantics of this resource; the semantics of setting a memory register.

**Resource**: expresses the **time-dependent evolution** of quantities of interest to the mission.

**Resources Types**:

**State**: the value of a resource/variable at an instant in time. E.g. "The state of the radioMode resource is ON"

**Simulation**: Plan simulation is the act of predicting the usage and behavior of mission resources, over time, under the influence of planned activities. Put differently, simulation is an analysis of the effects of a plan upon mission resources.

**Simulation Results**: the output of a Merlin simulation run. The simulation results include, simulated activity spans, their arguments (root activities and those arising from decomposition), resource profiles, and events.

**Task**: allows a mission model to describe **time-dependent processes** that affect mission state.

**Window**:

## Technologies

**API**: Application Programming Interface.

**AWS**: [Amazon Web Services](https://aws.amazon.com). Provides on-demand cloud computing platforms and API.

**CAM**: Common Access Manager. A [NASA AMMOS](https://ammos.nasa.gov/) utility which provides application layer access control capabilities, including single sign-on (SSO), federation, authorization management, authorization checking & enforcement, identity data retrieval, and associated logging

**Docker**: [Docker](https://www.docker.com/) is a set of platform as a service (PaaS) products that use OS-level virtualization to deliver software in packages called containers.

**GraphQL**: is an open-source data query and manipulation language for APIs, and a runtime for fulfilling queries with existing data. [graphql.org](https://graphql.org/)

**[Hasura](https://hasura.io/)**: a GraphQL schema compiler and GraphQL API server. As a compiler, Hasura parses a PostgresDB schema and generates a GraphQL schema defining a data graph of Queries, Mutation, and Subscriptions. Aerie utilizes a Hasura component to expose select database tables as the Aerie GraphQL API.

**JAR**: A [JAR](<https://en.wikipedia.org/wiki/JAR_(file_format)>) is a package file format typically used to aggregate many Java class files and associated metadata and resources into one file for distribution. JAR files are archive files that include a Java-specific manifest file. They are built on the ZIP format and typically have a .jar file extension.

**JUnit**: is a unit testing framework for the Java programming language. [junit.org](https://junit.org/junit5/)

**JVM**: A [Java virtual machine](https://en.wikipedia.org/wiki/Java_virtual_machine) is a virtual machine that enables a computer to run Java programs as well as programs written in other languages that are also compiled to Java bytecode.

**PostgreSQL**: [PostgreSQL](https://www.postgresql.org/), also known as Postgres, is a free and open-source relational database management system emphasizing extensibility and SQL compliance.

**SPICE**: NASA's Navigation and Ancillary Information Facility (NAIF) offers NASA flight projects and NASA funded researchers the "SPICE" observation geometry information system to assist scientists in planning and interpreting scientific observations from space-based instruments aboard robotic planetary spacecraft. In the Merlin context SPICE is most commonly incorporated to a mission model as a Java library. The library is configured with data files called "kernels" and then a mission model will query the library for an assortment of mission specific geometric data. [SPICE](https://naif.jpl.nasa.gov/naif/toolkit.html)

**Worker**: Aerie provides a multi-tenancy capability so that many users can run simulations concurrently. Simulation multi-tenancy is achieved by configuring Aerie to launch multiple simulation worker containers. Each simulation worker can execute a sand-boxed simulation run.
