# Overview

## Purpose

This documentation describes the Aerie GraphQL API software interface provided by the latest [Aerie release](https://github.com/NASA-AMMOS/aerie/releases).

## Interface

GraphQL is not a programming language capable of arbitrary computation, but is instead a language used to query application servers that have capabilities defined by the GraphQL specification. Clients use the GraphQL query language to make requests to a GraphQL service.

The three key GraphQL terms are;

- **Schema**: a type system which defines the data graph. The schema is the data sub-space over which queries can be defined.
- **Query**: a JSON-like read-only operation to retrieve data from a GraphQL service.
- **Mutation**: An explicit operation to effect server side data mutation.

A REST API architecture defines a particular URL endpoint for each "resources". In contrast, GraphQL's conceptual model is an entity graph. As a result, entities in GraphQL are not identified by URL endpoints and GraphQL is not a REST architecture API. Instead, a GraphQL server operates on a single endpoint, and all GraphQL requests for a given service are directed at this endpoint. Queries are constructed using the query language and then submitted as part of a HTTP request (either GET or POST).

The schema (graph) defines nodes and how they connect/relate to one another. A client composes a query specifying the fields to retrieve (or mutation for fields to create/update). A client develops their query/mutation with reference to the exposed GraphQL schema. As a result, a client develops custom queries/mutations targeted to its own use cases to fetch only the needed data from the API. In many cases this may reduce latency and increase performance by limiting client side data manipulation/filtering. For example, with a REST API there may be significant client side overhead and request latency when querying for an entire plan and then filtering the plan for the specific information of concern (and the work of adding filter fields as parameters to the end point). In contrast the GraphQL API allows the client to request only the fields of the plan data structure needed to satisfy the client's use case.

The Aerie GraphQL API is versioned with Aerie releases. However, a GraphQL based API gives greater flexibility to clients and Aerie when evolving the API. Adding fields and data to the schema does not affect existing queries. A client must specify the fields that make up their query. The additional fields in the graph simply play no part in the client’s composed query. As a result, additions to the API do not require updates on the client side. However, clients do need to deal with schema changes when fields are removed or type definitions are evolved. Furthermore, GraphQL makes possible per-field auditing of the frequency and combinations with which certain fields are referenced by a client’s queries and mutations. This provides Aerie developers with evidence of usage frequency which informs decision processes regarding deprecating or updating fields in the schema.

### GraphQL Query Fundamentals

A round trip usage of the API consists of the following three steps:

1. Composing a request (query or mutation)
2. Submitting the request via POST
3. Receiving the result as JSON

#### POST Request

A standard GraphQL HTTP POST request should use the `application/json` content type, and include a JSON-encoded body of the following form:

```json
{
  "query": "...",
  "operationName": "...",
  "variables": { "myVariable": "someValue" }
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

If there were no errors returned, the `"errors"` field is not present in the response. If no data is returned, the `"data"` field is only included if the error occurred during execution.

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
const response = await fetch("http://<your_domain_here>:8080/v1/graphql", {
  body: JSON.stringify({ query: "query { plan { id } }" }),
  headers: {
    Accept: "application/json",
    Authorization: "SSO_COOKIE_VALUE_HERE",
    "Content-Type": "application/json",
  },
  method: "POST",
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

A full description of these clients is found at [https://graphql.org/code/#language-support](https://graphql.org/code/#language-support)

## Schema

The schema is too large to include here and in Aerie's automatic documentation generation. The schema for your Aerie installation can be viewed at `http://<your_domain_here>:8080/console/api/api-explorer`.

## Usage

Aerie employs the [Hasura](https://hasura.io/) GraphQL engine to generate the Aerie GraphQL API.
It is important to understand the significance and power of a data graph based API. A small primer of the GraphQL syntax can be found [here](https://graphql.org/learn/schema/).

### Queries & Subscriptions

- [Postgres: GraphQL Queries](https://hasura.io/docs/latest/queries/postgres/index/)
- [Postgres: GraphQL Subscriptions](https://hasura.io/docs/latest/subscriptions/postgres/index/)
- [API Reference - Query / Subscription](https://hasura.io/docs/latest/api-reference/graphql-api/query/)

### Mutations

- [Postgres: GraphQL Mutations](https://hasura.io/docs/latest/mutations/postgres/index/)
- [API Reference - Mutation](https://hasura.io/docs/latest/api-reference/graphql-api/mutation/)
