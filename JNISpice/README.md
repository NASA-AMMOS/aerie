# JNISpice
Builds NAIF's JNISpice so that it can be used in the Merlin SDK and
Merlin-derived adaptations. This repository makes NAIF's library usable via
maven.

When future versions of JNISpice are released, follow these steps to update:
1. Empty this directory of all files except `pom.xml` and `settings.xml`.
2. Download NAIF’s JNI Spice for Mac at `https://naif.jpl.nasa.gov/pub/naif/misc/JNISpice_N0066/`.
3. Follow NAIF’s README instructions for installation + unzipping in a directory elsewhere on your computer.
4. Copy the `JNISpice/src/JNISpice` directory’s contents into this directory.
5. Create `src/main/java` directories (for maven) and move the `spice` directory into `src/main/java`.


