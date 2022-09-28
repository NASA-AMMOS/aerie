# Retrieve SeqJSON Serialization

Once a sequence is associated with activity instances, you can retrieve the SeqJson serialization of the sequence. This serialization is a simple concatenation of the commands associated with the activity instances in time-order. Any activities that do not have any associated commands (not yet expanded, no expansion associated, or some error during expansion) will not have any commands included in the resulting serialization. Additionally, any errors encountered during expansion will be included in the SeqJson as a command with the commands stem `$$ERROR$$`.

Retrieving the SeqJson serialization of a sequence is a simple API call
```
query GetSequenceSeqJson(
    $seqId: String!
    $simulationDatasetId: Int!
) {
    getSequenceSeqJson(
        seqId: $seqId,
        simulationDatasetId: $simulationDatasetId
    ) {
        id
        metadata
        steps {
            type
            stem
            args
            time {
                tag
                type
            }
            metadata
        }
    }
}
```
