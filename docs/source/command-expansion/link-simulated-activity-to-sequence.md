# Link Simulated Activity To Sequence

Once a sequence is created, you can link simulated activities to the sequence. Linking simulated activities to a sequence specifies that that activity's commands should be included in the sequence. Note that a simulated activity is NOT the same as an activity inserted in the plan, but the result of simulation on that plan and the ids are NOT interchangeable. You can retrieve the simulated activities for a simulation dataset to determine which to associated with the following query
```
query GetSimulatedActivities(
    $simulationDatasetId: Int!
) {
    simulated_activity(where: { simulation_dataset_id: {_eq: $simulationDatasetId } }) {
        id
        start_offset
        duration
        activity_type_name
        attributes
    }
}
```

Linking an activity to a sequence is then a simple API call using the id from above

```
mutation LinkSimulatedActivityToSequence(
    $seqId: String!
    $simulationDatasetId: Int!
    $simulatedActivityId: Int!
) {
    insert_sequence_to_simulated_activity_one(object: {
        seq_id: $seqId
        simulated_activity_id: $simulatedActivityId
        simulation_dataset_id: $simulationDatasetId
    }) {
        seq_id
    }
}
```

Note: A simulated activity can only be associated with one sequence
