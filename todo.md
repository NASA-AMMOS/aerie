# TODO: Persistent checkpoint
// MD: TODO for persistent checkpoint

- [ ] Save serialized cells to file
- [ ] Restore cells from file
- [ ] Formulate plan for deserializing tasks

## Problem statement
The purpose of this feature is to enable smooth extension of a plan into the following plan.

This involves saving simulation state at some given handover time, and being able to resume from it.

The resumption process specifies the following:
- Which checkpoint to use as a starting point (// MD: must this be the start of the next plan?)
- The new plan, which may only include activities in the future of the checkpoint
- A targeted list of in-progress tasks to terminate
- An indication of whether daemons should be restarted or resumed

// MD: A problem to consider: what happens when a mission model is updated? Can the checkpoints be salvaged?

## Architecture
Resuming from a checkpoint must restore the following information:


### Reasonable requirements, and flexible soundness guarantees // MD:
There may be tradeoffs between ergonomics and soundness. We strive for soundness, but if that results in draconian
requirements on the user, that may lead it to be unusable. In particular, around mission model updates, and the
serializability/resumability of tasks, there may be some room for opting in to unsoundness. For example:
- If it is better to start a new daemon task than to try to resume the previous one, perhaps that's not unreasonable.
- There may be the option to reset certain Cells to defaults
- There may be the need to make assumptions across mission model versions

There are also compromises to be made around when checkpoints can be taken:
- Maybe there's a notion of a safepoint, where no unserializable tasks are running

