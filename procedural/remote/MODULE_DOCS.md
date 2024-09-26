# Module Remote

This library provides tools for accessing plans and simulation results from remote execution environments.
This is intended to allow goal and constraint authors to run their code locally while remotely interacting
with an Aerie instance. These classes do not need to be packaged inside the jars, and instead should be
imported by a driver that the user implements themselves.

# Package gov.nasa.ammos.aerie.procedural.remote
Utilities for using goals and constraints to locally interact with a remote Aerie instance.
