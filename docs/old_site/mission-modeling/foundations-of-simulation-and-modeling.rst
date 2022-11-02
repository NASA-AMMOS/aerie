======================================
Foundations of Simulation and Modeling
======================================

**Merlin** is a mission modeling environment and hybrid system simulator based on the Java programming language.

Because mission models are expressed in Java, rather than a custom DSL, Merlin has little to no ability to see the actual Java code comprising a mission model. Merlin must instead make inferences about the mission model based on its observable behavior.

Predecessors of Merlin, such as APGen and SEQGen, provided a domain-specific language for mission modeling, allowing them to obtain deep, fine-grained information about the composition of a mission model before performing any simulation. In some ways, this provides enhanced ergonomics, as a mission modeler can focus on expressing their model directly in the modeling language, without being concerned with the needs of the system that will be interpreting that model. The language itself captures all interesting aspects of the model.

Unlike the DSLs of APGen and SEQGen, Java is a general-purpose language with no explicit provisions for mission modeling. To serve mission modeling, these facilities must instead be built *on top* of Java, forming a bridge between the mission model and the simulation system. A mission model must explicitly use this bridge to expose modeling knowledge to the system interpreting their model. It is this *modeling interface*, not the *authoring language*, that must express all interesting aspects of the model.

.. note::
  Aside: A language can be, and often is, construed as an interface in its own right. However, these linguistic interfaces
  are often so rich and complex that a difference in degree becomes a difference in kind. Most mainstream statically-typed
  languages, including Java, cannot faithfully embed linguistic interfaces in their type systems; any attempt quickly blows
  through the degree of expressivity provided by the type system.

  Languages like Haskell and Scala provide more expressive type systems, and dependently-typed languages like Idris are
  more expressive still. These programming languages allow a more faithful embedding of linguistic interfaces, so they are
  often used to support eDSLs (embedded domain-specific languages) in research and industry.

The dichotomy between the modeling interface and the authoring language bounds the design of the Merlin modeling experience between two extremes.

* At one extreme, the modeling interface dominates the experience of modeling, to the point that almost any authoring language could have been used as long as the interface could be embedded into it. This design is characterized by the intrusive presence of elements of the interface throughout the mission model, and is not much different from hand-writing the abstract syntax tree of a program in some domain-specific language.
* At the other extreme, the authoring language dominates the experience of modeling, and the interface avoids repeating capabilities that are already possessed by the authoring language. The interface is purely relegated to the role of "bridge", binding the relevant native entities to the intended domain concepts.

Merlin briefly explored the first extreme at its inception, with pervasive use of the Builder pattern to describe elements of the model. It quickly became apparent that this avoided most of the benefits of Java: common development tools like autocompletion could not be used to guide mission modelers, and the modeling experience was very unlike Java developmment in general. The early development of Merlin was characterized by a gradual shift away from this end of the design space.

Merlin has chosen to pursue the second extreme on principle, utilizing the authoring language to its greatest extent while augmenting it with domain-specific semantics where necessary. Among other reasons, Java was originally chosen as a modeling language because it would serve as a "transferable skill" for those both entering and exiting the "mission modeler" role. As a general-purpose language, Java provides a solid baseline for building a modular, maintainable system of any variety. High-performance Java runtimes already exist, and the oft-forgotten debugging experience is present out of the box. Merlin intends that mission modeling "taste" like development in Java more broadly, with the mission model "flavors" carefully integrated into that experience.

So what *is* the interface?
---------------------------

The Merlin interface must be minimal, to allow the authoring language to take center stage, while complete, to allow modeling knowledge to be transferred out of the mission model. A minimal interface can always be built upon the authoring language to provide more natural ergonomics. To that end, there are three primary landmarks in the Merlin mission modeling interface.

* **Cells** allow a mission model to express **time-dependent state** in a way that can be tracked and managed by the host system.
* **Resources** allow a mission model to express the **time-dependent evolution** of quantities of interest to the mission.
* **Tasks** allow a mission model to describe **time-dependent processes** that affect mission state.

As a common theme, the interface augments Java with *time-dependence*, allowing the flow of simulation time to be decoupled from (and queried independently of) the flow of real time. As a rule, we are not interested only in the state a model finds itself in at the end of a period of time, but rather the succession of all states it transitions through over time.

Cells
.....

In Java, every object begins in some state (upon construction); can be transitioned into another state by sending messages to it (via methods); and can be interrogated for information based on its current state (also methods). Even primitives in Java fit this mold: the value (state) of a primitive field may be replaced or retrieved wholesale. (Structures that behave like a primitive field are sometimes called "atomic registers".)

However, normal Java objects are not aware of the distinction between simulation time and real time. It is not possible to ask an arbitrary object about a state it previously inhabited, and it is even less possible to put an object into two states simultaneously, as occurs when simulation time splits and rejoins for concurrently-executing tasks.

**Cells** are a time-dependent generalization of mutable objects in Java supporting concurrent use across simultaneously-acting tasks, and retaining historical knowledge about its state at any simulation time. **All mutable state accessible during simulation must be manipulated through a containing cell.**

Like an object, a cell possesses an internal state and a set of operations to transition between states. These operations are called "effects", and are logged alongside the cell. The state of a cell at any simulation time is solely determined by its initial state and the effects upon it prior to that time.

Unlike an object, a cell possesses a simulation-aware semantics for combining sequential and concurrent effects and for explicitly transitioning the cell between states. **The state of a cell must not be affected except by applying effects to its containing cell.**

Through cells, a mission model may manage mutable state much as though it were a Java object, with behavior appropriate to the order of operations in simulation time, rather than the less predictable order of operations in real time. The concurrent semantics of Merlin simulation is confined to the internal behavior of cells, allowing for tight control of custom semantics while isolating the rest of the mission model from these concerns.

Through cells, the Merlin simulation system may observe when and which elements of simulation state are queried or affected as the simulation proceeds. This constitutes *the single most powerful tool* at Merlin's disposal to obtain insight into a mission model, as it allows Merlin to collect two different sets of knowledge over the course of simulation:

* A **simulation timeline** captures all effects on all cells in the order they occur, even accounting for concurrent effects between two simultaneous tasks. This structure completely captures the sense of the term "simulation time": the state of the mission model at any time of interest is fully determined by an index into this structure.
* A **dependency graph** captures all causal dependencies between cells, tasks, and resources. This allows the runtime system to optimize system execution in multiple ways, and it may also enable mission planners to, for instance, better understand how an earlier activity influences a later one.

A mission model may create a cell by providing an initial state and an effect semantics through the Merlin interface. It receives a handle to the cell in the shape of a Java object, and may then use it idiomatically like any other Java object.

Tasks
.....

In Java, an object transitions between states when a method is invoked on it. The methods of one object may recursively invoke the methods of other objects, causing an entire graph of objects to transition between states.

Methods in Java are not normally explicitly aware of the passage of time. They may ask the host system what time it is, but the amount of time that passes is not a functional element of the system -- rather, it is an incidental effect of the hardware and other software executing on the same host. Moreover, multiple methods in Java cannot proceed concurrently. At most one method is ever in progress, and it must complete before its caller -- and *only* its caller -- may proceed.

**Tasks** are a time-dependent generalization of methods in Java. A task may transition the model between states by performing effects upon its cells. A task may spawn other tasks, then proceed concurrently with its children -- concurrent effects are resolved by the cells to which they are posed. Tasks may explicitly await the passage of simulation time before continuing, or may await the completion of another task or the transition of the model into a particular state.

Like methods, tasks possess their own internal state, representing the work left to be completed by the task. Progress through a Java method is implicitly managed by the Java runtime (via the call stack); in order to use methods as a foundation for tasks, we must supplement this implicit state rather than replacing it.

The Merlin interface treats tasks in terms of *steps*. Every time a task is stepped forward, it updates its internal state, performs some effects, spawns some children, and then reports a *status* describing when to step the task again. The task's internal state is managed entirely on one side of the interface, and so does not need to be transmitted. In other words, a task is fundamentally treated as an opaque state machine.

However, *specifying* tasks as state machines requires interleaving modeling logic with tedious bookkeeping. This avoids many of the benefits of Java methods, and it isn't possible to pause a task while invoking other methods unless they are specified in the same way. To that end, we provide task specification platforms that centralize the tedious bookkeeping to a single context object against which task may invoke methods to spawn, delay, and perform effects. A task can be specified as a regular Java method that happens to have access to this context.

Note that these specification platforms are not part of the modeling interface itself; they merely adapt the state machine-oriented interface to the ergonomic expectations of users.

Directives
..........

Mission models are used by interacting with them in some way, and observing the resulting impact on the model state over time. While tasks specify how the model state itself is changed, **directives** specify the external stimuli which may be posed against a model.

The concept of directives has a different name depending on how the model is being used. When used in an activity planning workflow, directives represent the **activities** performed by the mission system. When used in a sequencing workflow, directives represent the **sequences** and **commands** dispatched by the ground station. In all cases, they cause the mission system to respond in some way -- in other words, to spawn a task.

The Merlin interface allows a mission model to register the directives it supports, along with a task to be spawned when a directive is received. Directives also specify a set of **parameters**, allowing the behavior of a directive to be modulated. The arguments for a specific directive are provided directly to the task it spawns.

As with most concepts in Java, method arguments are themselves named objects of some type. However, the arguments to a directive are specified by a mission planner, typically via a UI rather than Java source code. Thus, directive arguments must be serializable and deserializable to a model-agnostic form that can be presented ergonomically to a planner. The modeling interface provides all arguments in this form, and does not hard-code support for arbitrary Java types.

Working with this constrained data type poses a burden on mission modelers, who would need to interleave processing of these argument representations with their modeling logic. Moreover, planners would generally like to know ahead of time whether the arguments they've provided are valid for a given directive, rather than waiting until simulation time to observe a failure. To that end, mission modelers may separately provide -- and reuse existing definitions of -- dedicated value mappers, converting between values of the general-purpose interchange type and values of the desired modeling type.

Finally, when executing in the context of a directive, the mission model may spawn *other directives*. These directives are executed just like any other spawned task, but they can also be reported to the modeler as a product of simulation. This process, called **decomposition**, allows planners to better understand how a single directive breaks down into distinct behaviors, and the overall decomposition hierarchy is a primary input into the sequence generation workflow, which realizes an activity plan in a form suitable for execution by a physical system.

Resources
.........

In Java, the state of an object is "encapsulated", meaning it cannot be observed directly from the outside. Instead, the object exposes methods that return different information depending on its current state. Moreover, an object may itself reference other objects, so the state of an object may depend on the state of many others. A method may depend upon these references by recursively invoking other methods.

In Merlin, a modeled system transitions between states by reacting to discrete stimuli at instantaneous times. However, even a system in a fixed state can continuously affect its environment: a rocket that imparts a constant force over time will see its position and velocity change over time. Thus, we need the ability to ask about the behavior of the system not just at discrete times, but over continuous regions of time.

A **resource** is a time-dependent generalization of getter methods in Java. Resources provide information describing the steady-state behavior of some quantity over time, starting at the time at which it is queried and continuing indefinitely. This information is called a **dynamics**, as it describes the autonomous dynamical behavior of the resource.

Merlin currently supports two kinds of resource: discrete and real resources. The behavior of a discrete resource is given by a single fixed value: a discrete resource does not change autonomously. The behavior of a real resource is given by an initial value and slope, i.e. a line: a real resource accrues over time. (We hope to support general polynomial resources in the future.)

Although a resource dynamics describes an autonomous behavior, that behavior may change when the mission system transitions between states. Merlin simply re-queries any resources affected by the state transition to obtain their new autonomous dynamics.
