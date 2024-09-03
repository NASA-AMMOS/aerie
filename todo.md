# TODO: Persistent checkpoint
// MD: TODO for persistent checkpoint

- [x] Proof of concept: Save serialized cells to file
- [x] Proof of concept: Restore cells from file
- [ ] Properly manage files
- [ ] Design test case for save-and-restore without any tasks or activities
- [ ] Formulate plan for deserializing tasks
- [ ] Design test case for save-and-restore with tasks or activities

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
- All of the cells at the latest time
  - Restore both their values and their expiries
- All active tasks, in their current state
- The job queue
  - I think it may be sufficient to restore all tasks, and use their latest status to populate the job queue.
    This will produce all of the necessary conditions, delayed tasks, calling tasks
- The spans and span contributor counts

// MD: Should conditions be re-sampled upon resume, or should we cache their previous result/expiry? Should we cache what cells they referenced on their last sampling?


### Reasonable requirements, and flexible soundness guarantees // MD:
There may be tradeoffs between ergonomics and soundness. We strive for soundness, but if that results in draconian
requirements on the user, that may lead it to be unusable. In particular, around mission model updates, and the
serializability/resumability of tasks, there may be some room for opting in to unsoundness. For example:
- If it is better to start a new daemon task than to try to resume the previous one, perhaps that's not unreasonable.
- There may be the option to reset certain Cells to defaults
- There may be the need to make assumptions across mission model versions

There are also compromises to be made around when checkpoints can be taken:
- Maybe there's a notion of a safepoint, where no unserializable tasks are running

Warnings:
- We can attempt to serialize the initial values of all cells, and issue a warning if any did not override serialize or deserialize, or if deserialize(serialize(x)).equals(x) is not true. 

## Test procedure

1. Build and bring up Aerie
2. Stop aerie_merlin_worker_2 `docker compose stop aerie_merlin_worker_2`
3. Shell into aerie_merlin_worker_1 `docker exec -it aerie_merlin_worker_1 /bin/sh`
4. Upload mission model
5. Create plan 1 and 2 adjoining
6. Simulate plan 1
7. Simulate plan 2

Whenever going from plan 2 to plan 1, manually delete `fincons.json` via the open shell.


## TODO

- [x] Demonstrate serializable cells
- [ ] Demonstrate restarting activities
  - [x] Paused on Delay
    - [x] Save
    - [x] Resume
  - [x] Paused on Condition
    - [x] Save
    - [x] Resume
  - [ ] Paused on Call
    - [ ] Save
    - [ ] Resume
  - [ ] Performed a read
    - [ ] Save
    - [x] Resume
- [ ] Demonstrate restarting subtasks
- [ ] Demonstrate poking in new values
- [ ] Demonstrate conditionally omitting activities from being restarted
- [ ] Consider what to do if sim config affects what cells are allocated - can we either be robust to this, or forbid it?
- [ ] Demonstrate spans that continue from a previous plan
- [ ] Demonstrate multi-step task
- [ ] Investigate approaches to minimizing size - maybe gzip?
- [ ] Propagate across multiple simulations (i.e. populate readLog etc for long running tasks)
- [ ] Demonstrate anchors?
