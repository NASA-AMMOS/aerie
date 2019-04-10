[![Build Status](https://cae-jenkins2.jpl.nasa.gov/buildStatus/icon?job=MPSA/SEQ/nest/nest%20build/master)](https://cae-jenkins2.jpl.nasa.gov/job/MPSA/job/SEQ/job/nest/job/nest%20build/job/master/)
[![Quality Gate](https://seq-sca-mgss.jpl.nasa.gov/api/badges/measure?key=mgss.seq%3Anest&metric=ncloc)](https://seq-sca-mgss.jpl.nasa.gov/dashboard/index/com.qualinsight.plugins.sonarqube:qualinsight-plugins-sonarqube-badges)
[![Quality Gate](https://seq-sca-mgss.jpl.nasa.gov/api/badges/measure?key=mgss.seq%3Anest&metric=bugs)](https://seq-sca-mgss.jpl.nasa.gov/dashboard/index/com.qualinsight.plugins.sonarqube:qualinsight-plugins-sonarqube-badges)
[![Quality Gate](https://seq-sca-mgss.jpl.nasa.gov/api/badges/measure?key=mgss.seq%3Anest&metric=critical_violations)](https://seq-sca-mgss.jpl.nasa.gov/dashboard/index/com.qualinsight.plugins.sonarqube:qualinsight-plugins-sonarqube-badges)

# NEST

> NEST is the Mission Planning, Sequencing and Analysis (MPSA) Team's platform to provide a cohesive user experience to multi-mission users that require activity planning, sequencing, validation and analysis capabilities. The system's architecture is component-based and can be seized for different needs at different phases of a mission.

## What's inside NEST?

NEST hosts components that provide functionalities to work with adaptations of a mission's model, create sequences that command spacecrafts and visualizes points of interest to a mission, among many other things. The power of NEST is in its flexibility. Every single component in the application is a subject-matter expert in one, and only one, thing.

## Product Requirements

| Software Product           | Version |
| -------------------------- | ------- |
| NodeJS                     | 8.9.3   |
| NPM (Node Package Manager) | 5.5.1   |

**Supported Browsers**

| Name    | Version |
| ------- | ------- |
| Chrome  | latest  |
| Firefox | latest  |

**System Requirements**

| Hardware            | Details                                |
| ------------------- | -------------------------------------- |
| CPU                 | 2 gigahertz (GHz) frequency or above   |
| RAM                 | 4 GB at minimum                        |
| Display Resolution  | 2560-by-1600, recommended              |
| Internet Connection | High-speed connection, at least 10Mbps |

\*In order to provide meaningful data, the team recommends consuming it via MPSServer. This approach will evolve as services are made available, though.

## Usage

This project uses the [Angular CLI](https://cli.angular.io/). All dependencies should be kept in line with the latest version.

```
npm i
npm start
```

Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Configuration

As previously noted, NEST is a platform built out of applications, that are also built out of components. There are different deployment scenarios for various needs. A mission may decide to take all of the applications: planning, sequencing, and visualization, or any other combination, based on the needs. Another realistic scenario is when a mission needs some of the components, without the application. For this use-case we suggest the use of Web Components.

### Configuration/Environment Variables

**Environment-specific Build**

As a system administrator, you can set the environment to match the desired setup. The baseUrl should point to the host that will be providing the data, in this case, MPSServer's location:

`{NEST}/src/environments/environment.ts`

```typescript
export const environment = {
  baseUrl: 'https://leucadia.jpl.nasa.gov:9443',
  production: false,
};
```

**Basic Configuration Settings**

As previously noted, a mission may want to provide all or some of the applications available inside NEST. In order to configure the modules the configuration must be accessed and a build will follow. Comment/Uncomment the desired modules to display.

`{NEST}/src/config.ts`

```typescript
appModules: [
  {
    icon: 'event',
    path: 'plans',
    title: 'Planning',
    version: '0.0.1',
  },
  {
    icon: 'dns',
    path: 'sequencing',
    title: 'Sequencing',
    version: '0.0.1',
  },
  {
    icon: 'poll',
    path: 'raven',
    title: 'Visualization',
    version: version.packageJsonVersion,
  },
```

The configuration file contains other properties that can be modified. It is recommended to not change them. As the project evolves, new capabilities to change them via a remote configuration setting or a runtime configuration will be provided.

## Protocols

All of the communication that happens inside the application is through HTTP/HTTPS. The client modules talk to a backend service via HTTP.

## Development

### Code scaffolding

Run `npm run ng generate component component-name` to generate a new component. You can also use `npm run ng generate directive|pipe|service|class|guard|interface|enum|module`.

### Running unit tests

Run `npm test` (or `npm t`) to execute the unit tests via [Karma](https://karma-runner.github.io/).

## Deployment

In order to install NEST, for now, it needs to be served by MPSServer. There are two ways two deploy the set of applications. Both require a production build. Run `npm run build-prod` to build the project. The build artifacts will be stored in the `dist/` directory.

### Using Docker

Please follow the instructions in: https://github.jpl.nasa.gov/MPS/docker-raven

### Traditional Installation

Please follow the instructions in: https://github.jpl.nasa.gov/MPS/mps_server

## Support

### Defect Reporting Procedure

To report a defect in Nest, please create an ISA ticket and assign it to the SEQ Support Team. If you are unsure of the process, please send an email to [SEQ.support@jpl.nasa.gov](mailto:SEQ.support@jpl.nasa.gov). Within this message the following should be included:

- Title
- Brief Description of the defect and steps to reproduce
- Nest Version
- Criticality of the defect

### Points of Contact

| Type                | Point of Contact                                                                              |
| ------------------- | --------------------------------------------------------------------------------------------- |
| Administration      | Hector Acosta [hector.r.acosta@jpl.nasa.gov](mailto:hector.r.acosta@jpl.nasa.gov)             |
| General Help        | Hector Acosta [hector.r.acosta@jpl.nasa.gov](mailto:hector.r.acosta@jpl.nasa.gov)             |
| Technical Questions | Chris Camargo [Christopher.A.Camargo@jpl.nasa.gov](mailto:Christopher.A.Camargo@jpl.nasa.gov) |
|                     | Taifun O'Reilly [Taifun.L.OReilly@jpl.nasa.gov](mailto:Taifun.L.OReilly@jpl.nasa.gov)         |
|                     | Dustin Boston [dustin.m.boston@jpl.nasa.gov](mailto:dustin.m.boston@jpl.nasa.gov)             |

## Change Log

See [CHANGELOG](./CHANGELOG.md)

## Documentation Generation

Documentation for NEST is generated via [compodoc](https://compodoc.app/). Documentation is hosted on the [gh-pages](https://github.jpl.nasa.gov/MPS/aerie/tree/gh-pages) branch of the Aerie repository. To generate the documentation for the [develop](https://github.jpl.nasa.gov/MPS/aerie/tree/develop) branch, from the root of the Aerie repository do:

```
git checkout develop
git pull origin develop
git checkout gh-pages
git merge develop
cd nest
npm run docs
git push origin gh-pages
```

We need to do this anytime NEST is updated so the documentation is kept in sync with `develop`. You can view the docs from the `gh-pages` branch [here](https://github.jpl.nasa.gov/pages/MPS/aerie/nest/documentation/overview.html).
