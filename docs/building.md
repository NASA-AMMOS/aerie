# Building Aerie

## Pre-Requisites 

| Name    | Version | Notes        | Download                 |
|---------|---------|--------------|--------------------------|
| OpenJDK | 11.0.X  | HotSpot JVM  | https://adoptopenjdk.net |
| Gradle  | 6.X     | Build system | https://gradle.org       |

## Instructions

### Building

Run `gradle classes`.

### Testing

Run `gradle test`.

### Updating Dependencies

Run `gradle dependencyUpdates` to view a report of the project dependencies that are have updates available or are up-to-date.
