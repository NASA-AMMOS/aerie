# Aerie GraphQL API

## Purpose

This document describes the Aerie GraphQL API software interface provided by the latest [Aerie release](https://github.com/NASA-AMMOS/aerie/releases).

## Terminology and Notation

No special notation is used in this document.

## Interface Overview

GraphQL is not a programming language capable of arbitrary computation, but is instead a language used to query application servers that have capabilities defined by the GraphQL specification. Clients use the GraphQL query language to make requests to a GraphQL service.

The three key GraphQL terms are;

* **Schema**: a type system which defines the data graph. The schema is the data sub-space over which queries can be defined.
* **Query**: a JSON-like read-only operation to retrieve data from a GraphQL service.
* **Mutation**: An explicit operation to effect server side data mutation.

A REST API architecture defines a particular URL endpoint for each "resources". In contrast, GraphQL's conceptual model is an entity graph. As a result, entities in GraphQL are not identified by URL endpoints and GraphQL is not a REST architecture API. Instead, a GraphQL server operates on a single endpoint, and all GraphQL requests for a given service are directed at this endpoint. Queries are constructed using the query language and then submitted as part of a HTTP request (either GET or POST).

The schema (graph) defines nodes and how they connect/relate to one another. A client composes a query specifying the fields to retrieve (or mutation for fields to create/update). A client develops their query/mutation with reference to the exposed GraphQL schema. As a result, a client develops custom queries/mutations targeted to its own use cases to fetch only the needed data from the API. In many cases this may reduce latency and increase performance by limiting client side data manipulation/filtering. For example, with a REST API there may be significant client side overhead and request latency when querying for an entire plan and then filtering the plan for the specific information of concern (and the work of adding filter fields as parameters to the end point). In contrast the GraphQL API allows the client to request only the fields of the plan data structure needed to satisfy the client's use case.

The Aerie GraphQL API is versioned with Aerie releases. However, a GraphQL based API gives greater flexibility to clients and Aerie when evolving the API. Adding fields and data to the schema does not affect existing queries. A client must specify the fields that make up their query. The additional fields in the graph simply play no part in the client’s composed query. As a result, additions to the API do not require updates on the client side. However, clients do need to deal with schema changes when fields are removed or type definitions are evolved. Furthermore, GraphQL makes possible per-field  auditing of the frequency and combinations with which certain fields are referenced by a client’s queries and mutations. This provides Aerie developers with evidence of usage frequency which informs decision processes regarding deprecating or updating fields in the schema.

### GraphQL Query Fundamentals

A round trip usage of the API consists of the following three steps:

1. Composing a request (query or mutation)
2. Submitting the request via POST
3. Receiving the result as JSON

#### POST Request

A standard GraphQL HTTP POST request should use the `application/json` content type, and include a JSON-encoded body of the following form:

```json
{
  "query":  "...",
  "operationName":  "...",
  "variables": { "myVariable":  "someValue" }
}
```

`operationName` and `variables` are optional fields. The `operationName` field is only required if multiple operations are present in the query.

#### Response

Regardless of the method by which the query and variables are sent, the response is returned in the body of the request in JSON format. A query’s results may include some data and some errors, and those are returned in a JSON object of the form:
```json
{
  "data": {},
  "errors": []
}
```

If there were no errors returned, the `"errors"` field is not present in the response. If no data is returned, the `"data"` field is only  included if the error occurred during execution.

### GraphQL Clients

Since a GraphQL API has more underlying structure than a REST API, there are a range of methods by which a client application may choose to interact with the API.
A simple usage could use the `curl` command line tool, whereas a full featured web application may integrate a more powerful client library like [Apollo Client](https://www.apollographql.com/docs/react/) or [Relay](https://relay.dev/) which automatically handle query building, batching and caching.

#### Command Line

One may build and send a query or mutation via any means that enable an HTTP POST request to be made against the API. For example, this can be easily done using the command line tool [Graphqurl](https://github.com/hasura/graphqurl).

#### GraphQL Console

The GraphQL API is described by a schema of the data graph. One can view the schema of the installed version of Aerie at `http://<your_domain_here>:8080/console/api/api-explorer`. The GraphQL console allows one to compose and test queries.

#### Browser Developer Console

Requests can also be tested using JavaScript from a web-browser. For example the following JavaScript can be used to make a simple query using the [fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API):

```js
const response = await fetch('http://<your_domain_here>:8080/v1/graphql', {
  body: JSON.stringify({ query: 'query { plan { id } }' }),
  headers: {
    Accept: 'application/json',
    Authorization: 'SSO_COOKIE_VALUE_HERE',
    'Content-Type': 'application/json',
  },
  method: 'POST',
});
const data = await response.json();
console.log(data);
```

Here is an example of a possible shape of the data logged to the console:

```
{
  "data": {
    "plans": [{ "id": 1 }, { "id": 2 }]
  }
}
```

This JavaScript can then be used as a hard-coded query within a client tool/script. For more complex and dynamic interactions with the Aerie API it is recommended to use a GraphQL client library.

#### Client Libraries

When developing a full featured application that requires integration with the Aerie API it is advisable that the tool make use of one of the many powerful GraphQL client libraries like Apollo Client or Relay. These libraries provide an application functionality to manage both local and remote data, automatically handle batching, and caching.

In general, it will take more time to set up a GraphQL client. However, when building an Aerie integrated application, a client library offers significant time savings as the features of the application grow. One might choose to begin using HTTP requests as the client's API integration mechanism and later switch to a client library as the application becomes more complex.

GraphQL clients exist for the following programming languages:

- C# / .NET
- Clojurescript
- Elm
- Flutter
- Go
- Java / Android
- JavaScript
- Julia
- Kotlin
- Swift / Objective-C iOS
- Python
- R

A full description of these clients is found at [https://graphql.org/code/#graphql-clients](https://graphql.org/code/#graphql-clients)

## Aerie GraphQL API

### Schema 

The schema is too large to include here and in Aerie's automatic documentation generation. The schema for your Aerie installation can be viewed at `http://<your_domain_here>:8080/console/api/api-explorer`.

### Usage

Aerie employs the [Hasura](https://hasura.io/) GraphQL engine to generate the Aerie GraphQL API.
It is important to understand the significance and power of a data graph based API. A small primer of the GraphQL syntax can be found [here](https://graphql.org/learn/schema/).

#### Queries & Subscriptions

- [Postgres: GraphQL Queries](https://hasura.io/docs/latest/queries/postgres/index/)
- [Postgres: GraphQL Subscriptions](https://hasura.io/docs/latest/subscriptions/postgres/index/)
- [API Reference - Query / Subscription](https://hasura.io/docs/latest/api-reference/graphql-api/query/)

#### Mutations

- [Postgres: GraphQL Mutations](https://hasura.io/docs/latest/mutations/postgres/index/)
- [API Reference - Mutation](https://hasura.io/docs/latest/api-reference/graphql-api/mutation/)

### Examples

The following queries are examples of what Aerie refers to as "canonical queries" because they map to commonly understood use cases and data structures within mission subsystems.
When writing a GraphQL query, refer to the schema for all valid fields that one can specify in a particular query.

#### Query for All Plans

```
query {
  plan {
    duration
    id
    model_id
    name
    start_time
  }
}
```

#### Query a Single Plan

```
query {
  plan_by_pk(id: 1) {
    duration
    id
    model_id
    name
    start_time
  }
}
```

#### Query for All Activity Instances for a Plan

You can either query `plan_by_pk` for all activity instances for a single plan, or query `plan` for all activity instances from *all* plans.

The following query returns all activity instances for a single plan:

```
query {
  plan_by_pk(id: 1) {	
    activity_directives {
      arguments
      id
      type
    } 
  }
}
```

#### Query the Mission Model of a Plan

```
query {
  plan_by_pk(id: 1) {	
    mission_model {
      id
      mission
      name
      owner
      version
    }
  }
}
```

#### Query for All Activity Types within a Mission Model of a Plan

Returns a list of activity types. For each activity type, the name, parameter schema, which parameters are required (must be defined).

```
query {
  plan_by_pk(id: 1) {	
    mission_model {
      activity_types {
        computed_attributes_value_schema
        name
        parameters
        required_parameters
      }
    }
  }
}
```

#### Run Simulation

The following query starts a simulation, or returns information if the simulation has already started. The `simulationDatasetId` can be used to query for simulation results.

```
query { 
  simulate(planId: 1) {
    reason
    simulationDatasetId
    status
  }
}
```

#### Query for All Simulated Activities in a Simulated Plan

```
query {
  plan_by_pk(id: 1) {
    simulations(limit: 1) {
      datasets(order_by: { id: desc }, limit: 1) {
        simulated_activities {
          activity_type_name
          attributes
          duration
          id
          parent_id
          simulation_dataset_id
          start_offset
        }
      }
    }
    start_time
  }
}
```

#### Query for All Resource Profiles in Simulated Plan

Profiles are simulated resources. The following query gets profiles for a given plan's latest simulation dataset (i.e. the latest resource simulation results):

```
query {
  plan_by_pk(id: 1) {
    duration
    simulations(limit: 1) {
      datasets(order_by: { id: desc }, limit: 1) {
        dataset {
          profiles {
            name
            profile_segments {
              dynamics
              start_offset
            }
            type
          }
        }
      }
    }
    start_time
  }
}
```

#### Query for All Simulated Activities and Resource Profiles in Simulated Plan

The following query just combines the previous two queries to get all activities and profiles in a simulated plan: 

```
query {
  plan_by_pk(id: 1) {
    duration
    simulations(limit: 1) {
      datasets(order_by: { id: desc }, limit: 1) {
        simulated_activities {
          activity_type_name
          attributes
          duration
          id
          parent_id
          simulation_dataset_id
          start_offset
        }
        dataset {
          profiles {
            name
            profile_segments {
              dynamics
              start_offset
            }
            type
          }
        }
      }
    }
    start_time
  }
}
```

#### Query for All Resource Samples in Simulated Plan

```
query {
  resourceSamples(planId: 1) {
    resourceSamples
  }
}
```

#### Query for All Constraint Violations in Simulated Plan

```
query {
  constraintViolations(planId: 1) {
    constraintViolations
  }
}
```

#### Query for All Resource Types in a Mission Model

```
query {
  resourceTypes(missionModelId: 1) {
    name
    schema
  }
}
```

#### Create Plan

```
mutation {
  insert_plan_one(
    object: {
      duration: "432000 seconds 0 milliseconds"
      model_id: 1
      name: "My First Plan"
      start_time: "2020-001T00:00:00"
    }
  ) {
    id
    revision
  }
}
```

#### Create Simulation

Each plan must have a least one associated simulation to execute a simulation. To create a simulation for a plan you can use the following mutation:

```
mutation {
  insert_simulation_one(
    object: { arguments: {}, plan_id: 1, simulation_template_id: null }
  ) {
    id
  }
}
```

#### Create Activity Instances (Directives)

```
mutation {
  insert_activity_directive(
    objects: [
      {
        arguments: { peelDirection: "fromTip" }
        plan_id: 1
        start_offset: "1749:01:35.575"
        type: "PeelBanana"
      }
      {
        arguments: { peelDirection: "fromTip" }
        plan_id: 1
        start_offset: "1750:01:35.575"
        type: "PeelBanana"
      }
    ]
  ) {
    returning {
      id
      start_offset
    }
  }
}
```

#### Query for Activity Effective Arguments

This query returns a set of effective arguments given a set of required (and overridden) arguments.

```
query {
  getActivityEffectiveArguments(
    missionModelId: 1
    activityTypeName: "BakeBananaBread"
    activityArguments: { tbSugar: 1, glutenFree: false }
  ) {
    arguments
    errors
    success
  }
}
```

Resulting in:

```json
{
  "data": {
    "getActivityEffectiveArguments": {
      "arguments": {
        "temperature": 350,
        "tbSugar": 1,
        "glutenFree": false
      },
      "success": true
    }
  }
}
```

When a required argument is not provided, the returned JSON will indicate which argument is missing. With `examples/banananation`'s `BakeBananaBread`, where only the `temperature` parameter has a default value:

```
query {
  getActivityEffectiveArguments(
    missionModelId: 1
    activityTypeName: "BakeBananaBread"
    activityArguments: {}
  ) {
    arguments
    errors
    success
  }
}
```

Results in:

```json
{
  "data": {
    "getActivityEffectiveArguments": {
      "arguments": {
        "temperature": 350
      },
      "errors": {
        "tbSugar": {
          "schema": {
            "type": "int"
          },
          "message": "Required argument for activity \"BakeBananaBread\" not provided: \"tbSugar\" of type ValueSchema.INT"
        },
        "glutenFree": {
          "schema": {
            "type": "boolean"
          },
          "message": "Required argument for activity \"BakeBananaBread\" not provided: \"glutenFree\" of type ValueSchema.BOOLEAN"
        }
      },
      "success": false
    }
  }
}
```

#### Query for Mission Model Configuration Effective Arguments

The `getModelEffectiveArguments` returns the same structure as `getActivityEffectiveArguments`; a set of effective arguments given a set of required (and overridden) arguments. For example, `examples/config-without-defaults`'s has all required arguments:

```
query {
  getModelEffectiveArguments(missionModelId: 1, modelArguments: {}) {
    arguments
    errors
    success
  }
}
```

Results in:

```json
{
  "data": {
    "getModelEffectiveArguments": {
      "arguments": {},
      "errors": {
        "a": {
          "schema": {
            "type": "int"
          },
          "message": "Required argument for configuration \"Configuration\" not provided: \"a\" of type ValueSchema.INT"
        },
        "b": {
          "schema": {
            "type": "real"
          },
          "message": "Required argument for configuration \"Configuration\" not provided: \"b\" of type ValueSchema.REAL"
        },
        "c": {
          "schema": {
            "type": "string"
          },
          "message": "Required argument for configuration \"Configuration\" not provided: \"c\" of type ValueSchema.STRING"
        }
      },
      "success": false
    }
  }
}
```
