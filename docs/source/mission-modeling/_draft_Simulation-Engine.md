# Simulation Engine (DRAFT!)

Introduce activities as the a means for providing directives to the sim engine
Events 
Pat to write up event presentation
Motivating examples 
Timeline
Timeline mechanics
Cells
Events (out of the engine)
Matt writing up event outputting 







Target audience: a mission modeler
A mission modeler is a software engineer - we make no further assumptions about experience or knowledge.

Some terms to be used here:
An Activity

Merlin is a discrete event simulator with “next-event time progression”. https://en.wikipedia.org/wiki/Discrete-event_simulation 

The simulation state at any time can be computed from the history of events that took place before that time.

Notions of time within the simulation:
Real time (discrete counter of microseconds since simulation start)
Dense time (causal relationships between simultaneous events)

The simulation engine takes a “schedule” of “tasks” and executes them from time zero until the end time.

A schedule assigns each activity to a Real time.

A paused Task is always in one of the following states:
Delayed
AwaitingTask
AwaitingCondition
Completed

Right now, activities are the only kind of “Task”, but in the future, “Commands” will be too.

Introducing Schedules, Activities, and Events
For the purposes of this document, we are drawing no distinction between the “Activities” in a plan and “Tasks” in the simulation engine.

A schedule is a set of activities, each of which has a start time. This time is specified in some client-specific way, but by the time it gets to the simulation engine, it is an integer number of microseconds from the start time of the simulation.

In this document, we are representing a schedule as rectangles spaced out on a horizontal line. The rectangles are meant to represent activities, and the space between them is meant to represent time passing. Note that the duration of activities (i.e. the width of the rectangles) is not relevant to this present discussion.

[[images/sim-engine/image5.png]]


The only way that activities can affect simulation state is by emitting events. The information contained in events is entirely model-specific - the simulation engine merely stores these events and provides access to them when the simulation state related to a particular event type is queried.

Let’s have these blue rectangles emit blue circles, representing events. Note that to an observer later in the simulation, the simulation state can be affected by all of the events that came before it.

So far, it appears that a timeline could be represented simply as a set of events and their corresponding timestamps. Let’s delve a bit deeper - what happens if more than one event is emitted at the same timestamp?

Activities can happen at the same time
Imagine we create a schedule in which two activities start at the same time. Which event should come first in the timeline? We want it to be clear to future observers that these events happened in parallel.

[[images/sim-engine/image12.png]]

Still, we can represent this scenario as a set of events and timestamps - the observer can assume that all events that happen at the same timestamp happened in parallel. Let’s look at a scenario in which this representation would lose important information about the simulation.
Activities can emit events immediately after other events
Let’s add an orange activity that waits for a blue event, and then emits an orange event.

When we start the orange activity, it will wait idly.
When the next blue activity starts, it emits a blue event.
The orange activity is immediately unpaused, after which it emits an orange event.

It is important to note that no real time passes between the blue and orange events - they are emitted during the same timestamp.

[[images/sim-engine/image13.png]]

It is important to an observer which event happened first. Perhaps the orange event is related to the same piece of simulation state as the blue event - that state would appear differently to a future observer if the order of the events were to be reversed, or if they were to be treated as happening in parallel.

We’ve established that storing a timestamp with each event is not enough information to recover the order in which the events were emitted.

Let’s further explore the different shapes that these event timelines can take, so we can better qualify what information needs to be stored in order to recover the history of a simulation.

Activities can spawn other activities
Let’s throw decomposition into the mix. An activity (we’ll call it the parent activity) can spawn another activity (we’ll call it the child activity). The child activity starts at the same timestamp at which it is spawned. The parent activity continues in parallel with its child.

Let’s define a green activity which starts by emitting a green event, then spawns a blue activity.

We start the green activity
The green activity emits a green event
The green activity spawns a blue activity
The blue activity emits a blue event

The green event occurs strictly before the blue event, although both occur at the same timestamp.

[[images/sim-engine/image7.png]]

Now, let’s tweak the definition of the green activity: what if we reversed the order of spawn and emit, so that the green activity first spawns the blue activity, then emits the green event?

- We start the green activity
- The green activity spawns a blue activity
- The green activity emits a green event and the blue activity emits a blue event

In this scenario, there is no way to know whether the green event or the blue event comes first, because the parent and child activity are occurring in parallel.

[[images/sim-engine/image1.png]]

Now, let’s combine the last two possibilities and show a scenario where events are emitted both before and after a spawn. The purple activity emits a purple event, then spawns a green activity.

We start the purple activity
- The purple activity emits a purple event
- The purple activity spawns a green activity
- The green activity spawns a blue activity
- The green activity emits a green event and the blue activity emits a blue event

As we’ve seen before, the event emitted before the spawn occurs before events emitted after the spawn. The two events that occur after all the spawns occur in parallel. We draw lines from the purple event to the blue and green events to represent this branching structure.

[[images/sim-engine/image4.png]]

What does this branching structure mean for the state of the simulation? An activity that occurs at a future timestamp would see that both green and blue occurred in parallel. The big question is: immediately after emitting the blue event, does the blue activity see the green event?

[[images/sim-engine/image11.png]]

During one timestep, activities obey transactional semantics. This means that two activities executing in parallel are unaffected by each other's events until they both yield.

You can extend this ad nauseum - these trees can get arbitrarily deep.

[[images/sim-engine/image3.png]]

Event Graphs
Over the past several sections, we’ve explored some shapes that simulation events may take, and we’ve arrived at a branching structure that lets us track which events occur concurrently, and which events occur sequentially. This structure is tree-like in nature, though as we’ve observed there can be multiple parallel branches from the very beginning, so it’s more of like a forest. These edges are directed, since the direction of time matters. Thus, we can think about events occurring at a single timestamp as a directed forest:

[[images/sim-engine/image8.png]]

We can artificially join the roots of the trees to unite them into one structure:

[[images/sim-engine/image2.png]]

Let’s introduce a little bit of notation: these “artificial nodes” which exist only to provide structure can be labeled. Let’s use a “C” to mark concurrently nodes whose children occur in parallel, and “S” to mark sequentially nodes whose children occur in sequence. The above tree can be rewritten like this:

[[images/sim-engine/image15.png]]

Take a moment to convince yourself that this tree is equivalent to the forest above. We’re going to be using this representation for the rest of this section.

Now, let’s use this new representation to label each event with its position in the tree. Our numbering scheme is as follows: for all children of a particular node, use the parent node’s label as a prefix, and as a suffix number each child from left to right.

[[images/sim-engine/image6.png]]

Having labeled all of our events with their place in the tree, we don’t need the artificial nodes any more. Actually we don’t need any structure at all! All the structure is encoded in the label.

[[images/sim-engine/image9.png]]

We can recover the original structure from these labels by repeatedly grouping by prefix and alternating between sequential and concurrent layers.

[[images/sim-engine/image10.png]]

Not only can we recover the structure of the whole tree, but we can determine the relationships between any set of events. This is powerful, since it lets us look at parts of the simulation at a time. You can imagine filtering events by type, or even selecting all events that occur between two events.

Selecting all events between two events would involve

[[images/sim-engine/image14.png]]

