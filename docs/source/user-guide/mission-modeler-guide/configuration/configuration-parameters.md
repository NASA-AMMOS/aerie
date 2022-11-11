## Configuration Parameters

A configuration class should be defined with the same parameter annotations as activities.
See [Advanced Topic: Parameters](../parameters/index)
for a thorough explanation of all possible styles of `@Export` parameter declarations.

Similarly to activities, the Merlin annotation processor will take care of all serialization/deserialization of the configuration object.
The Merlin annotation processor will generate a configuration mapper for the configuration defined within `@WithConfiguration()`.

