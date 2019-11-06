import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor.ActivityProcessor;

import javax.annotation.processing.Processor;

module gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk {
  requires static com.squareup.javapoet;
  requires transitive static jdk.compiler;

  requires com.fasterxml.jackson.core;
  requires commons.math3;
  requires java.desktop;
  requires java.scripting;
  requires JNISpice;
  requires org.apache.commons.lang3;

  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.representation;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.annotations;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.engine;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.processor;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time;
  exports gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.utilities;

  provides Processor with ActivityProcessor;

  uses MerlinAdaptation;
}
