# Create A Sequence

Sequence creations starts with declaring a new sequence on a simulation dataset. Because of the 
complexities with activities in the plans regenerating on simulation, sequences are tied to a 
specific simulation run rather than a plan (future work will explore addressing this).

The API call to do this is a simple mutation

```
mutation CreateSequence(
    $seqId: String!
    $simulationDatasetId: Int!
) {
    insert_sequence_one(object: {
        simulation_dataset_id: $simulationDatasetId,
        seq_id: $seqId,
        metadata: {},
    }) {
        seq_id
    }
}
```

Where the seqId is the id you want assigned to the sequence and simulationDatasetId is the id of 
the simulation dataset for the simulation you wish to create the sequence for.

Note: You can only have one of any given seqId for a given simulationDataset
