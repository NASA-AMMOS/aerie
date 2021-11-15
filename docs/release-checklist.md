# Aerie Release Checklist

## Generate Test Report
- [ ] Generate a report which includes the latest test run for each of the following test suites.
	- Nightly Aerie
	- Nightly Aerie UI Unit
- [ ] Download the test report as a PDF from Testrail and upload to Release Report Wiki page.

## Github
- [ ] Increment the Aerie product version number
- [ ] Merge develop into staging by creating a PR for the merge
- [ ] Remove the "SNAPSHOT" string from the staging branch product version number
- [ ] Bump version in `scripts/docker-compose-aerie/.env`
- [ ] Create a branch named "release-X.X.X" from staging for each of the following projects
  - aerie
  - [aerie-ui](https://github.jpl.nasa.gov/Aerie/aerie-ui/blob/develop/docs/RELEASE.md)
  - [aerie-apollo](https://github.jpl.nasa.gov/Aerie/aerie-apollo/blob/develop/docs/RELEASE.md)
  - [aerie-code-server](https://github.jpl.nasa.gov/Aerie/aerie-code-server/blob/develop/docs/RELEASE.md)
  - [aerie-editor](https://github.jpl.nasa.gov/Aerie/aerie-editor/blob/develop/docs/RELEASE.md)

## Maven/Artifacory
- [ ] Check out the release-X.X.X branch and publish the distributed JARs
	- for each of the published JARs (those with the `publishing{}` entries in their package's `build.gradle`) change the `url` entry to refer to `maven-libs-release-local` rather than the default `maven-libs-snapshot-local`.
	- run `./gradlew build publish` to publish the JARs
- [ ] Tag the release-X.X.X branch with "X.X.X"
- [ ] Push the release-X.X.X branch to origin

## Documentation Generation
- The documentation is automatically generated and uploaded to gh-pages
- [ ] Edit the index.html on the gh-pages branch to include the links to the automatically generated documentation.

## Release Report in Wiki
- [ ] Copy final version of Percent Complete Report tables into Release Report page.
- [ ] Check urls for all delivered products in artifactory
- For non-milestone revisions (0.7.1, 0.7.2, ...):
  - [ ] Move staged release notes from the next milestone's Release Report into the present Release Report page.

## Send Email To Stakeholders
- [ ] Edit the following email text to point to the correct artifacts.

>Hello Aerie customers and stakeholders,
>
>Aerie X.X.X release is available.
>
>Artifacts that make up the release are available at JPL Artifactory under these directories:
>
>docker-release-local/gov/nasa/jpl/aerie/**merlin-server**/release-X.X.X
>docker-release-local/gov/nasa/jpl/**aerie-apollo**/release-X.X.X
>docker-release-local/gov/nasa/jpl/**aerie-ui**/release-X.X.X
>
>Aerie provides three libraries that can be imported by a project from JPL Artifactory. They are,
>-   **merlin-sdk** at maven-libs-release-local/gov/nasa/jpl/aerie/merlin-sdk/X.X.X/
>-   **activity-processor** at maven-libs-release-local/gov/nasa/jpl/aerie/activity-processor/X.X.X/
>-   **contrib** at maven-libs-release-local/gov/nasa/jpl/aerie/contrib/X.X.X/
>-   **merlin-framework-junit** at maven-libs-release-local/gov/nasa/jpl/aerie/merlin-framework-junit/X.X.X/
>Aerie Sequence Editor (Falcon):
>
>general/gov/nasa/jpl/aerie/aerie-editor-release-X.X.X.tar.gz
>
>Product Guide:
>
>[https://github.jpl.nasa.gov/pages/Aerie/aerie/X.X.X/wiki/index.html#Product-Guide](https://github.jpl.nasa.gov/pages/Aerie/aerie/X.X.X/wiki/index.html#Product-Guide)
>
>Deployment instructions are linked from the top of the page.
>
>User Guide
>
>[https://github.jpl.nasa.gov/pages/Aerie/aerie/X.X.X/wiki/index.html#User-Guide](https://github.jpl.nasa.gov/pages/Aerie/aerie/X.X.X/wiki/index.html#User-Guide)
>
>Mission Modeler (Adaptation Guide) Guide
>
>[https://github.jpl.nasa.gov/pages/Aerie/aerie/X.X.X/wiki/index.html#Mission-Modeler-Guide](https://github.jpl.nasa.gov/pages/Aerie/aerie/X.X.X/wiki/index.html#Mission-Modeler-Guide)
>
>Release Report and all release documentation can be found at MPSA confluence wiki here:
>
>[https://wiki.jpl.nasa.gov/display/MPSA/Aerie+X.X.X+Release+Report](https://wiki.jpl.nasa.gov/display/MPSA/Aerie+X.X.X+Release+Report)
>
>If you have any questions about Aerie you can reach out to us at [aerie\_support@jpl.nasa.gov](mailto:aerie_support@jpl.nasa.gov) address.
