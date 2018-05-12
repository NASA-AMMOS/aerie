[![Build Status](https://cae-jenkins2.jpl.nasa.gov/buildStatus/icon?job=MPSA/SEQ/raven2/raven2%20build/master)](https://cae-jenkins2.jpl.nasa.gov/job/MPSA/job/SEQ/job/raven2/job/raven2%20build/job/master/) 
[![Quality Gate](https://seq-sca-mgss.jpl.nasa.gov/api/badges/measure?key=mgss.seq%3Araven2&metric=ncloc)](https://seq-sca-mgss.jpl.nasa.gov/dashboard/index/com.qualinsight.plugins.sonarqube:qualinsight-plugins-sonarqube-badges)
[![Quality Gate](https://seq-sca-mgss.jpl.nasa.gov/api/badges/measure?key=mgss.seq%3Araven2&metric=bugs)](https://seq-sca-mgss.jpl.nasa.gov/dashboard/index/com.qualinsight.plugins.sonarqube:qualinsight-plugins-sonarqube-badges)
[![Quality Gate](https://seq-sca-mgss.jpl.nasa.gov/api/badges/measure?key=mgss.seq%3Araven2&metric=critical_violations)](https://seq-sca-mgss.jpl.nasa.gov/dashboard/index/com.qualinsight.plugins.sonarqube:qualinsight-plugins-sonarqube-badges)

# Raven2

Resource and Activity Visualization Engine v2.

Raven2 is a web-based platform included in the SEQ subsystem of the Advanced Multi-mission Operations System (AMMOS) and managed by the Multi-mission Ground System and Services (MGSS). It allows users to view science planning, spacecraft activities, resource usage and predicted data, or any time-based data, displayed in a timeline format via web browser. Subsequently, it can be viewed simultaneously by distributed users/teams for collaboration when creating, updating and validating activity plans and command sequences.

## Develop

```
npm i
npm start
```

Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `npm run ng generate component component-name` to generate a new component. You can also use `npm run ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `npm run build-prod` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `npm test` (or `npm t`) to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `npm run e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `npm run ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
